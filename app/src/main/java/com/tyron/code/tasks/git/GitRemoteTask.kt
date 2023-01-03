package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import android.widget.Toast
import com.tyron.code.databinding.BaseTextinputLayoutBinding
import org.eclipse.jgit.lib.StoredConfig
import com.tyron.common.SharedPreferenceKeys
import android.content.SharedPreferences
import com.tyron.code.ApplicationLoader

object GitRemoteTask {
      
       val sharedPreferences: SharedPreferences = ApplicationLoader.getDefaultPreferences()
    
       fun remote(project:Project, context:Context) {
    
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
       
       val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
       binding.textinputLayout.setHint(R.string.remote_name)
       val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.title_add_remove_remote)
       builder.setView(binding.root)
       builder.setPositiveButton(R.string.add) { _, _ ->
         
       val future =
       executeAsyncProvideError( {
       val remote = binding.textinputLayout.editText?.text?.toString()
 
       if (remote.isNullOrBlank()) {
       ThreadUtils.runOnUiThread { Toast.makeText(context, context.getString(R.string.empty_remote), Toast.LENGTH_SHORT).show() }           
       } else {  
    
       val userName  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_NAME,"")  
       val url : String = "git@github.com:"+ userName.toString()+"/" + project.getRootFile().getName()+ ".git"
       val  config : StoredConfig = Git.open(project.getRootFile()).getRepository().getConfig()
       config.setString("remote", remote, "url", url)
       config.setString("remote", remote, "fetch", "+refs/heads/*:refs/remotes/" + remote +"/*");
       config.save() 
             
       }
       
       return@executeAsyncProvideError
       }, { _, _ -> } )  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) { ErrorOutput.ShowError(error, context) }  
       else { Toast.makeText(context,context.getString(R.string.remote_added_successfully),Toast.LENGTH_SHORT).show() }
       } } 
       
       }
       
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.setNeutralButton(R.string.remove) { _, _ -> 
       
       val future =
       executeAsyncProvideError( {
       val remote = binding.textinputLayout.editText?.text?.toString()
 
       if (remote.isNullOrBlank()) {
       ThreadUtils.runOnUiThread { Toast.makeText(context, context.getString(R.string.empty_remote), Toast.LENGTH_SHORT).show() }           
       } else {  
    
       val  config : StoredConfig = Git.open(project.getRootFile()).getRepository().getConfig()
       config.unsetSection("remote", remote);
       config.save()
       
       }
       
       return@executeAsyncProvideError
       }, { _, _ -> } )  
       
       future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
      
       if (result == null || error != null) { ErrorOutput.ShowError(error, context) }  
       else { Toast.makeText(context,context.getString(R.string.remote_removed_successfully),Toast.LENGTH_SHORT).show() }
       } } 
       
        }
       builder.show()    
    
}
}
