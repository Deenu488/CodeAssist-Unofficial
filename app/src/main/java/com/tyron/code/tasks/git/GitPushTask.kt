package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.tyron.code.databinding.LayoutDialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.ui.ssh.callback.SshTransportConfigCallback
import com.tyron.code.tasks.git.GitProgressMonitor

object GitPushTask {
  
       val sshTransportConfigCallback =  SshTransportConfigCallback()
     
       fun push(project:Project, context:Context) {
    
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)  
       val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
       val view = binding.root   
       binding.message.visibility = View.VISIBLE 
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.pushing)
       builder.setView(view)
       builder.setCancelable(false)     
      
       val progress = GitProgressMonitor(binding.progress, binding.message)
                            
       val future =
       executeAsyncProvideError( {
        
       Git.open(project.getRootFile()).push().setProgressMonitor(progress).setTransportConfigCallback(sshTransportConfigCallback).call()
    
       return@executeAsyncProvideError
       }, { _, _ -> } )  
      
       val dialog = builder.show() 
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
       dialog?.dismiss()
       
       if (result == null || error != null) {   ErrorOutput.ShowError(error, context)  }
       else {  Toast.makeText(context, context.getString(R.string.push_completed),Toast.LENGTH_SHORT).show() }     
    
       }}        
  
}
}
