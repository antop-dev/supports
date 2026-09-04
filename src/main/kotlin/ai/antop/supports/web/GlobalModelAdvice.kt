package ai.antop.supports.web

import ai.antop.supports.config.AppProperties
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/** 모든 화면에 공통으로 필요한 모델을 주입한다. */
@ControllerAdvice
class GlobalModelAdvice(
    private val appProperties: AppProperties,
) {
    @ModelAttribute
    fun addCommonAttributes(model: Model) {
        model.addAttribute("maxFiles", appProperties.upload.maxFiles)
        model.addAttribute("maxFileSizeBytes", appProperties.upload.maxFileSizeBytes)
    }
}
