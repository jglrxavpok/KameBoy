package org.jglrxavpok.kameboy.ui

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.nuklear.*
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.util.*


abstract class NuklearWindow(val title: String) {

    private var ALLOCATOR: NkAllocator = NkAllocator.create().alloc {handle, old, size -> nmemAllocChecked(size)}.mfree { handle, ptr -> nmemFree(ptr)}

    private val ttfBytes = javaClass.getResourceAsStream("/fonts/Consolas.ttf").readBytes()
    private val ttf = BufferUtils.createByteBuffer(ttfBytes.size).put(ttfBytes).flip() as ByteBuffer
    private var windowHandle: Long = -1
    protected lateinit var context: NkContext

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    protected var displayWidth: Int = 0
    protected var displayHeight: Int = 0

    private val default_font = NkUserFont.create() // This is the Nuklear font object used for rendering text.

    private val cmds = NkBuffer.create() // Stores a list of drawing commands that will be passed to OpenGL to render the interface.
    private val null_texture = NkDrawNullTexture.create() // An empty texture used for drawing.

    /**
     * The following variables are used for OpenGL.
     */
    private var vbo: Int = 0
    private var vao: Int = 0
    private var ebo: Int = 0
    private var prog: Int = 0
    private var vert_shdr: Int = 0
    private var frag_shdr: Int = 0
    private var uniform_tex: Int = 0
    private var uniform_proj: Int = 0

    private val BUFFER_INITIAL_SIZE = 4 * 1024

    private val MAX_VERTEX_BUFFER = 512 * 1024
    private val MAX_ELEMENT_BUFFER = 128 * 1024

    private var VERTEX_LAYOUT: NkDrawVertexLayoutElement.Buffer = NkDrawVertexLayoutElement.create(4)
            .position(0).attribute(Nuklear.NK_VERTEX_POSITION).format(Nuklear.NK_FORMAT_FLOAT).offset(0)
            .position(1).attribute(Nuklear.NK_VERTEX_TEXCOORD).format(Nuklear.NK_FORMAT_FLOAT).offset(8)
            .position(2).attribute(Nuklear.NK_VERTEX_COLOR).format(Nuklear.NK_FORMAT_R8G8B8A8).offset(16)
            .position(3).attribute(Nuklear.NK_VERTEX_ATTRIBUTE_COUNT).format(Nuklear.NK_FORMAT_COUNT).offset(0)
            .flip()

    abstract val defaultWidth: Int
    abstract val defaultHeight: Int

    private fun setupContext(allocator: NkAllocator) {
        val NK_SHADER_VERSION = if (Platform.get() === Platform.MACOSX) "#version 150\n" else "#version 300 es\n"
        val vertex_shader = NK_SHADER_VERSION +
                "uniform mat4 ProjMtx;\n" +
                "in vec2 Position;\n" +
                "in vec2 TexCoord;\n" +
                "in vec4 Color;\n" +
                "out vec2 Frag_UV;\n" +
                "out vec4 Frag_Color;\n" +
                "void main() {\n" +
                "   Frag_UV = TexCoord;\n" +
                "   Frag_Color = Color;\n" +
                "   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
                "}\n"
        val fragment_shader = NK_SHADER_VERSION +
                "precision mediump float;\n" +
                "uniform sampler2D Texture;\n" +
                "in vec2 Frag_UV;\n" +
                "in vec4 Frag_Color;\n" +
                "out vec4 Out_Color;\n" +
                "void main(){\n" +
                "   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                "}\n"

        nk_buffer_init(cmds, allocator, BUFFER_INITIAL_SIZE.toLong())
        prog = glCreateProgram()
        vert_shdr = glCreateShader(GL_VERTEX_SHADER)
        frag_shdr = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(vert_shdr, vertex_shader)
        glShaderSource(frag_shdr, fragment_shader)
        glCompileShader(vert_shdr)
        glCompileShader(frag_shdr)
        if (glGetShaderi(vert_shdr, GL_COMPILE_STATUS) !== GL_TRUE) {
            throw IllegalStateException()
        }
        if (glGetShaderi(frag_shdr, GL_COMPILE_STATUS) !== GL_TRUE) {
            throw IllegalStateException()
        }
        glAttachShader(prog, vert_shdr)
        glAttachShader(prog, frag_shdr)
        glLinkProgram(prog)
        if (glGetProgrami(prog, GL_LINK_STATUS) !== GL_TRUE) {
            throw IllegalStateException()
        }

        uniform_tex = glGetUniformLocation(prog, "Texture")
        uniform_proj = glGetUniformLocation(prog, "ProjMtx")
        val attrib_pos = glGetAttribLocation(prog, "Position")
        val attrib_uv = glGetAttribLocation(prog, "TexCoord")
        val attrib_col = glGetAttribLocation(prog, "Color")

        run {
            // buffer setup
            vbo = glGenBuffers()
            ebo = glGenBuffers()
            vao = glGenVertexArrays()

            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)

            glEnableVertexAttribArray(attrib_pos)
            glEnableVertexAttribArray(attrib_uv)
            glEnableVertexAttribArray(attrib_col)

            glVertexAttribPointer(attrib_pos, 2, GL_FLOAT, false, 20, 0)
            glVertexAttribPointer(attrib_uv, 2, GL_FLOAT, false, 20, 8)
            glVertexAttribPointer(attrib_col, 4, GL_UNSIGNED_BYTE, true, 20, 16)
        }

        run {
            // null texture setup
            val nullTexID = glGenTextures()

            null_texture.texture().id(nullTexID)
            null_texture.uv().set(0.5f, 0.5f)

            glBindTexture(GL_TEXTURE_2D, nullTexID)
            stackPush().use { stack -> glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(-0x1)) }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        }

        glBindTexture(GL_TEXTURE_2D, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    private fun initNkContext(): NkContext {
        val nkContext = NkContext.create()
        nk_init(nkContext, ALLOCATOR, null)
        return nkContext
    }

    open fun init() {
        this.context = initNkContext()
        this.width = defaultWidth
        this.height = defaultHeight
        this.displayWidth = width
        this.displayHeight = height
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL)
        glfwMakeContextCurrent(windowHandle)
        GL.createCapabilities()

        glfwSetScrollCallback(windowHandle) { window, xoffset, yoffset ->
            stackPush().use { stack ->
                val scroll = NkVec2.mallocStack(stack)
                        .x(xoffset.toFloat())
                        .y(yoffset.toFloat())
                nk_input_scroll(context, scroll)
            }
        }
        glfwSetCharCallback(windowHandle) { window, codepoint -> nk_input_unicode(context, codepoint) }
        glfwSetKeyCallback(windowHandle) { window, key, scancode, action, mods ->
            val press = action == GLFW_PRESS
            when (key) {
                GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true)
                GLFW_KEY_DELETE -> nk_input_key(context, NK_KEY_DEL, press)
                GLFW_KEY_ENTER -> nk_input_key(context, NK_KEY_ENTER, press)
                GLFW_KEY_TAB -> nk_input_key(context, NK_KEY_TAB, press)
                GLFW_KEY_BACKSPACE -> nk_input_key(context, NK_KEY_BACKSPACE, press)
                GLFW_KEY_UP -> nk_input_key(context, NK_KEY_UP, press)
                GLFW_KEY_DOWN -> nk_input_key(context, NK_KEY_DOWN, press)
                GLFW_KEY_HOME -> {
                    nk_input_key(context, NK_KEY_TEXT_START, press)
                    nk_input_key(context, NK_KEY_SCROLL_START, press)
                }
                GLFW_KEY_END -> {
                    nk_input_key(context, NK_KEY_TEXT_END, press)
                    nk_input_key(context, NK_KEY_SCROLL_END, press)
                }
                GLFW_KEY_PAGE_DOWN -> nk_input_key(context, NK_KEY_SCROLL_DOWN, press)
                GLFW_KEY_PAGE_UP -> nk_input_key(context, NK_KEY_SCROLL_UP, press)
                GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> nk_input_key(context, NK_KEY_SHIFT, press)
                GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> if (press) {
                    nk_input_key(context, NK_KEY_COPY, glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_PASTE, glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_CUT, glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_UNDO, glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_REDO, glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_LINE_START, glfwGetKey(window, GLFW_KEY_B) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_TEXT_LINE_END, glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS)
                } else {
                    nk_input_key(context, NK_KEY_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS)
                    nk_input_key(context, NK_KEY_COPY, false)
                    nk_input_key(context, NK_KEY_PASTE, false)
                    nk_input_key(context, NK_KEY_CUT, false)
                    nk_input_key(context, NK_KEY_SHIFT, false)
                }
            }
        }
        glfwSetCursorPosCallback(windowHandle) { window, xpos, ypos -> nk_input_motion(context, xpos.toInt(), ypos.toInt()) }
        glfwSetMouseButtonCallback(windowHandle) { window, button, action, mods ->
            stackPush().use { stack ->
                val cx = stack.mallocDouble(1)
                val cy = stack.mallocDouble(1)

                glfwGetCursorPos(window, cx, cy)

                val x = cx.get(0).toInt()
                val y = cy.get(0).toInt()

                val nkButton: Int
                when (button) {
                    GLFW_MOUSE_BUTTON_RIGHT -> nkButton = NK_BUTTON_RIGHT
                    GLFW_MOUSE_BUTTON_MIDDLE -> nkButton = NK_BUTTON_MIDDLE
                    else -> nkButton = NK_BUTTON_LEFT
                }
                nk_input_button(context, nkButton, x, y, action == GLFW_PRESS)
            }
        }
        context.clip {
            it
                    .copy { handle, text, len ->
                        if (len == 0) {
                            return@copy
                        }

                        stackPush().use { stack ->
                            val str = stack.malloc(len + 1)
                            memCopy(text, memAddress(str), len.toLong())
                            str.put(len, 0.toByte())

                            glfwSetClipboardString(windowHandle, str)
                        }
                    }
                    .paste { handle, edit ->
                        val text = nglfwGetClipboardString(windowHandle)
                        if (text != NULL) {
                            nnk_textedit_paste(edit, text, nnk_strlen(text))
                        }
                    }
        }
        setupContext(ALLOCATOR)

        val BITMAP_W = 1024
        val BITMAP_H = 1024

        val FONT_HEIGHT = 18
        val fontTexID = glGenTextures()

        val fontInfo = STBTTFontinfo.create()
        val cdata = STBTTPackedchar.create(95)

        var scale: Float = 0f
        var descent: Float = 0f

        stackPush().use { stack ->
            stbtt_InitFont(fontInfo, ttf)
            scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT.toFloat())

            val d = stack.mallocInt(1)
            stbtt_GetFontVMetrics(fontInfo, null, d, null)
            descent = d.get(0) * scale

            val bitmap = memAlloc(BITMAP_W * BITMAP_H)

            val pc = STBTTPackContext.mallocStack(stack)
            stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL)
            stbtt_PackSetOversampling(pc, 4, 4)
            stbtt_PackFontRange(pc, ttf, 0, FONT_HEIGHT.toFloat(), 32, cdata)
            stbtt_PackEnd(pc)

            // Convert R8 to RGBA8
            val texture = memAlloc(BITMAP_W * BITMAP_H * 4)
            for (i in 0 until bitmap.capacity()) {
                texture.putInt(bitmap.get(i).toInt() shl 24 or 0x00FFFFFF)
            }
            texture.flip()

            glBindTexture(GL_TEXTURE_2D, fontTexID)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)

            memFree(texture)
            memFree(bitmap)
        }

        default_font
                .width { handle, h, text, len ->
                    var text_width = 0f
                    stackPush().use { stack ->
                        val unicode = stack.mallocInt(1)

                        var glyph_len = nnk_utf_decode(text, memAddress(unicode), len)
                        var text_len = glyph_len

                        if (glyph_len == 0) {
                            return@width 0f
                        }

                        val advance = stack.mallocInt(1)
                        while (text_len <= len && glyph_len != 0) {
                            if (unicode.get(0) == NK_UTF_INVALID) {
                                break
                            }

                            /* query currently drawn glyph information */
                            stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null)
                            text_width += advance.get(0) * scale

                            /* offset next glyph */
                            glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len)
                            text_len += glyph_len
                        }
                    }
                    text_width
                }
                .height(FONT_HEIGHT.toFloat())
                .query { handle, font_height, glyph, codepoint, next_codepoint ->
                    stackPush().use { stack ->
                        val x = stack.floats(0.0f)
                        val y = stack.floats(0.0f)

                        val q = STBTTAlignedQuad.mallocStack(stack)
                        val advance = stack.mallocInt(1)

                        stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false)
                        stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null)

                        val ufg = NkUserFontGlyph.create(glyph)

                        ufg.width(q.x1() - q.x0())
                        ufg.height(q.y1() - q.y0())
                        ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent))
                        ufg.xadvance(advance.get(0) * scale)
                        ufg.uv(0).set(q.s0(), q.t0())
                        ufg.uv(1).set(q.s1(), q.t1())
                    }
                }
                .texture {
                    it
                            .id(fontTexID)
                }

        nk_style_set_font(context, default_font)
        glfwShowWindow(windowHandle)
    }

    fun tick() {
        if(!glfwWindowShouldClose(windowHandle)) {
            glfwMakeContextCurrent(windowHandle)
            glClear(GL_COLOR_BUFFER_BIT)
            glClearColor(1f, 0f, 0f, 1f)
            stackPush().use {
                // Create a rectangle for the window
                val rect = NkRect.mallocStack(it)
                rect.x(0f).y(0f).w(displayWidth.toFloat()).h(displayHeight.toFloat())
                // Begin the window
                if (nk_begin(context, title, rect, 0)) {
                    renderWindow(it)
                }
                nk_end(context)
            }

            renderNuklearContent()
            glfwSwapBuffers(windowHandle)
        }
    }

    private fun renderNuklearContent() {
        val ctx = context
        val win = windowHandle
        /* Determine the size of our GLFW window */
        stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)

            glfwGetWindowSize(win, w, h)
            width = w.get(0)
            height = h.get(0)

            glfwGetFramebufferSize(win, w, h)
            displayWidth = w.get(0)
            displayHeight = h.get(0)
        }

        /* Listen for mouse events */
        nk_input_begin(ctx)
        glfwPollEvents()

        val mouse = ctx.input().mouse()
        if (mouse.grab()) {
            glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
        } else if (mouse.grabbed()) {
            val prevX = mouse.prev().x()
            val prevY = mouse.prev().y()
            glfwSetCursorPos(win, prevX.toDouble(), prevY.toDouble())
            mouse.pos().x(prevX)
            mouse.pos().y(prevY)
        } else if (mouse.ungrab()) {
            glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
        }

        nk_input_end(ctx)

        /* End listening for mouse events */

        /* Nuklear function calls */
        //
        // Everything mentioned in this guide will go here.
        // I recommend putting all of that code into a method.
        //
        /* End Nuklear function calls */

        /* The following code draws the result to the screen */
        stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)

            glfwGetWindowSize(win, width, height)
            glViewport(0, 0, width.get(0), height.get(0))

        }
        glClear(GL_COLOR_BUFFER_BIT)
        /*
             * IMPORTANT: `nk_glfw_render` modifies some global OpenGL state
             * with blending, scissor, face culling, depth test and viewport and
             * defaults everything back into a default state.
             * Make sure to either a.) save and restore or b.) reset your own state after
             * rendering the UI.
             */
        render(NK_ANTI_ALIASING_ON, MAX_VERTEX_BUFFER, MAX_ELEMENT_BUFFER)
    }

    private fun render(AA: Int, max_vertex_buffer: Int, max_element_buffer: Int) {
        val ctx = context
        stackPush().use { stack ->
            // setup global state
            glEnable(GL_BLEND)
            glBlendEquation(GL_FUNC_ADD)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glDisable(GL_CULL_FACE)
            glDisable(GL_DEPTH_TEST)
            glEnable(GL_SCISSOR_TEST)
            glActiveTexture(GL_TEXTURE0)

            // setup program
            glUseProgram(prog)
            glUniform1i(uniform_tex, 0)
            glUniformMatrix4fv(uniform_proj, false, stack.floats(
                    2.0f / width, 0.0f, 0.0f, 0.0f,
                    0.0f, -2.0f / height, 0.0f, 0.0f,
                    0.0f, 0.0f, -1.0f, 0.0f,
                    -1.0f, 1.0f, 0.0f, 1.0f
            ))
            glViewport(0, 0, displayWidth, displayHeight)
        }

        run {
            // convert from command queue into draw list and draw to screen

            // allocate vertex and element buffer
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)

            glBufferData(GL_ARRAY_BUFFER, max_vertex_buffer.toLong(), GL_STREAM_DRAW)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, max_element_buffer.toLong(), GL_STREAM_DRAW)

            // load draw vertices & elements directly into vertex + element buffer
            val vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, max_vertex_buffer.toLong(), null))
            val elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, max_element_buffer.toLong(), null))
            stackPush().use { stack ->
                // fill convert configuration
                val config = NkConvertConfig.callocStack(stack)
                        .vertex_layout(VERTEX_LAYOUT)
                        .vertex_size(20)
                        .vertex_alignment(4)
                        .null_texture(null_texture)
                        .circle_segment_count(22)
                        .curve_segment_count(22)
                        .arc_segment_count(22)
                        .global_alpha(1.0f)
                        .shape_AA(AA)
                        .line_AA(AA)

                // setup buffers to load vertices and elements
                val vbuf = NkBuffer.mallocStack(stack)
                val ebuf = NkBuffer.mallocStack(stack)

                nk_buffer_init_fixed(vbuf, vertices/*, max_vertex_buffer*/)
                nk_buffer_init_fixed(ebuf, elements/*, max_element_buffer*/)
                nk_convert(ctx, cmds, vbuf, ebuf, config)
            }
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER)
            glUnmapBuffer(GL_ARRAY_BUFFER)

            // iterate over and execute each draw command
            val fb_scale_x = displayWidth.toFloat() / width.toFloat()
            val fb_scale_y = displayHeight.toFloat() / height.toFloat()

            var offset = NULL
            var cmd = nk__draw_begin(ctx, cmds)
            while (cmd != null) {
                if (cmd.elem_count() == 0) {
                    cmd = nk__draw_next(cmd, cmds, ctx)
                    continue
                }
                glBindTexture(GL_TEXTURE_2D, cmd.texture().id())
                glScissor(
                        (cmd.clip_rect().x() * fb_scale_x).toInt(),
                        ((height - (cmd.clip_rect().y() + cmd.clip_rect().h()).toInt()) * fb_scale_y).toInt(),
                        (cmd.clip_rect().w() * fb_scale_x).toInt(),
                        (cmd.clip_rect().h() * fb_scale_y).toInt()
                )
                glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset)
                offset += (cmd.elem_count() * 2).toLong()
                cmd = nk__draw_next(cmd, cmds, ctx)
            }
            nk_clear(ctx)
        }

        // default OpenGL state
        glUseProgram(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        glDisable(GL_BLEND)
        glDisable(GL_SCISSOR_TEST)
    }

    fun setPosition(x: Int, y: Int) {
        glfwSetWindowPos(windowHandle, x, y)
    }

    abstract fun renderWindow(stack: MemoryStack)


    fun nk_tab(context: NkContext, title: String, active: Boolean): Boolean {
        val font = context.style().font()
        stackPush().use { stack ->
            val ptr = stack.mallocPointer(1)
            ptr.put(memASCII(title))
            ptr.flip()
            val textWidth = font!!.width()!!.invoke(font.userdata().ptr(), font.height(), ptr[0], nk_strlen(title))
            val widgetWidth = textWidth + 6 * context.style().button().padding().x()
            nk_layout_row_push(context, widgetWidth)
            val oldStyle = context.style().button().normal()
            if(active) {
                context.style().button().normal(context.style().button().active())
            }
            val clicked = nk_button_label(context, title)
            context.style().button().normal(oldStyle)
            return clicked
        }
    }
}