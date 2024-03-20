package $packagename

import android.app.Activity
import android.os.Bundle

public class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)
    }
}