package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import java.io.File
import com.tyron.code.databinding.BaseTextinputLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.ApplicationLoader
import com.tyron.common.SharedPreferenceKeys
import android.content.SharedPreferences

object GitCommitTask {

       val sharedPreferences: SharedPreferences = ApplicationLoader.getDefaultPreferences()
    
       fun commit(project:Project, context:Context) {
   
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
       
       val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
       binding.textinputLayout.setHint(R.string.commit_message)
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.commit_changes)
       builder.setView(binding.root)
       builder.setPositiveButton(R.string.title_commit) { _, _ ->
       
       val userName  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_NAME,"")  
       val userEmail  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_EMAIL,"")  
      
       val future =
       executeAsyncProvideError( {
        
       val msg = binding.textinputLayout.editText?.text?.toString()      
       var file = File(project.getRootFile(), "/.git")
       var path = file.toString()
      
       if (userName.isNullOrBlank() && userEmail.isNullOrBlank()) {
        ThreadUtils.runOnUiThread { Toast.makeText(context,context.getString(R.string.set_user_and_password),Toast.LENGTH_SHORT).show()  }           
       } else if (msg.isNullOrBlank()) {
        ThreadUtils.runOnUiThread { Toast.makeText(context,context.getString(R.string.empty_commit),Toast.LENGTH_SHORT).show() }           
       }  else {
    
       Git.open(project.getRootFile()).commit().setAll(true).setCommitter(userName.toString(), userEmail.toString()) .setMessage(msg) .call()
       
       ThreadUtils.runOnUiThread { Toast.makeText(context,"Committed all changes to repository in " + path,Toast.LENGTH_SHORT).show()  }           

       }       
           
       return@executeAsyncProvideError
           }, { _, _ -> } ) 
            
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) {  ErrorOutput.ShowError(error, context)  }   
       } }
       
       }
       
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.show()
    
}
}
