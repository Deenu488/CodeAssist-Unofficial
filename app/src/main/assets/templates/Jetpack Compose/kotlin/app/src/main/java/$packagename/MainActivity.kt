package $packagename

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.activity.compose.setContent
import android.os.Bundle

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		// This is an extension function of Activity that sets the @Composable function that's
		// passed to it as the root view of the activity. This is meant to replace the .xml file
		// that we would typically set using the setContent(R.id.xml_file) method. The setContent
		// block defines the activity's layout.
		setContent {
			// Column is a composable that places its children in a vertical sequence. You
			// can think of it similar to a LinearLayout with the vertical orientation. 
			// In addition we also pass a few modifiers to it.
			
			// You can think of Modifiers as implementations of the decorators pattern that are used to
			// modify the composable that its applied to. In the example below, we configure the
			// Column to occupy the entire available height & width using Modifier.fillMaxSize().
			Column(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally,
				content = {
					// Here, SimpleText is a Composable function which is going to describe the contents of
					// this activity that will be rendered on the screen.
					SimpleText(
						"Hello world!",
						TextStyle(
							color = Color.Red,
							fontStyle = FontStyle.Italic,
							
						)
					)
				}
			)
		}
    }
}

// We represent a Composable function by annotating it with the @Composable annotation. Composable
// functions can only be called from within the scope of other composable functions.
@Composable
fun SimpleText(
	displayText: String,
	style: TextStyle = TextStyle.Default
) {
    // We should think of composable functions to be similar to lego blocks - each composable
    // function is in turn built up of smaller composable functions. Here, the Text() function is
    // pre-defined by the Compose UI library; you call that function to declare a text element
    // in your app.
	Text(
		text = displayText,
		style = style
	)
}