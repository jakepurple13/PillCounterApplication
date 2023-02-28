import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.programmersbox.common.UIShow
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        BrowserViewportWindow("PillCounter") {
            MaterialTheme(darkColorScheme()) {
                UIShow()
            }
        }
    }
}