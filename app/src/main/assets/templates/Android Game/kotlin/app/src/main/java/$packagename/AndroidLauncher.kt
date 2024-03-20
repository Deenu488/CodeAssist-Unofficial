package $packagename

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/* Launches the Android application. */
class AndroidLauncher : AndroidApplication() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		/* You can adjust this configuration to fit your needs */
		val configuration = AndroidApplicationConfiguration()
		configuration.useImmersiveMode = true
		initialize(GameSuperClass(), configuration)
	}
}