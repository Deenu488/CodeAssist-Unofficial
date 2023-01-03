package com.tyron.code.tasks.git

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context

object  ErrorOutput {
    
       fun ShowError(error: Throwable, context:Context) {
     
       val builder = MaterialAlertDialogBuilder(context)
       builder.setMessage(error.localizedMessage)
       builder.show()
    } 
    
}
