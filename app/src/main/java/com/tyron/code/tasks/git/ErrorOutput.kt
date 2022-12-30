package com.tyron.code.tasks.git

import com.tyron.code.ApplicationLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object  ErrorOutput {
    
       fun ShowError(error: Throwable) {
       val builder = MaterialAlertDialogBuilder(ApplicationLoader.applicationContext)
       builder.setMessage(error.localizedMessage)
       builder.show()
    } 
    
}
