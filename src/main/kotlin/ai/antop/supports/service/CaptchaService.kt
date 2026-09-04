package ai.antop.supports.service

import ai.antop.supports.config.AppProperties
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.imageio.ImageIO

/**
 * Java2D(Graphics2D) + BufferedImage + SecureRandom + ImageIO 로 숫자 캡차를 직접 생성한다.
 */
@Service
class CaptchaService(
    private val appProperties: AppProperties,
) {
    private val random = SecureRandom()

    data class Result(
        val code: String,
        val png: ByteArray,
    )

    fun generate(): Result {
        val config = appProperties.captcha
        val code = (1..config.length).joinToString("") { random.nextInt(10).toString() }
        val png = render(code, config.width, config.height)
        return Result(code, png)
    }

    private fun render(
        code: String,
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 배경
        g.color = Color(245, 245, 245)
        g.fillRect(0, 0, width, height)

        // 배경 노이즈 점
        repeat(width * height / 20) {
            g.color = randomColor(160, 230)
            g.fillRect(random.nextInt(width), random.nextInt(height), 1, 1)
        }

        // 숫자 그리기(회전/위치 변형)
        val baseFontSize = height * 3 / 5
        val cellWidth = (width - 20) / code.length
        for ((index, ch) in code.withIndex()) {
            val font = Font(Font.SANS_SERIF, Font.BOLD, baseFontSize + random.nextInt(10))
            g.font = font
            g.color = randomColor(20, 120)
            val angle = (random.nextDouble() - 0.5) * 0.6
            val x = 12 + index * cellWidth
            val y = height / 2 + baseFontSize / 3 + random.nextInt(6) - 3
            val transform = AffineTransform.getRotateInstance(angle, x.toDouble(), y.toDouble())
            g.transform = transform
            g.drawString(ch.toString(), x, y)
            g.transform = AffineTransform()
        }

        // 방해선
        g.stroke = BasicStroke(1.5f)
        repeat(5) {
            g.color = randomColor(120, 200)
            g.drawLine(
                random.nextInt(width),
                random.nextInt(height),
                random.nextInt(width),
                random.nextInt(height),
            )
        }

        g.dispose()

        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    private fun randomColor(
        min: Int,
        max: Int,
    ): Color {
        val range = max - min
        return Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range))
    }
}
