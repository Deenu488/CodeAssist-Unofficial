package com.tyron.code.ui.git

import android.os.Build
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.tyron.code.R
import com.tyron.code.databinding.BaseTextinputLayoutBinding
import com.tyron.code.databinding.LayoutDialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Environment
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import com.tyron.code.util.executeAsyncProvideError
import android.widget.TextView
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.eclipse.jgit.api.CloneCommand
import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Status
import android.widget.Toast
import com.tyron.code.ui.ssh.callback.SshTransportConfigCallback
import org.eclipse.jgit.transport.FetchResult
import com.tyron.common.SharedPreferenceKeys
import android.content.SharedPreferences
import com.tyron.code.ApplicationLoader
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.api.PullCommand

object GitClone {

       val sshTransportConfigCallback =  SshTransportConfigCallback()
       var mResult:  StringBuffer  =  StringBuffer()
       val sharedPreferences: SharedPreferences = ApplicationLoader.getDefaultPreferences()
    
       fun cloneGitRepo(context:Context) {
   	   val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint(R.string.git_clone_repo_url)
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle(R.string.git_clone_repo)
	   builder.setView(binding.root)
	   builder.setCancelable(true)
       builder.setPositiveButton(R.string.git_clone) { dialog, _ ->
       dialog.dismiss()
       val url = binding.textinputLayout.editText?.text?.toString()
       cloneRepo(url, context)
       }
       builder.setNegativeButton(android.R.string.cancel, null)
   
       builder.show()
       }			
	   private fun cloneRepo(repo: String?, context:Context) {
       if (repo.isNullOrBlank()) {
       return
       }

       var url = repo.trim()
       if (!url.endsWith(".git")) {
       url += ".git"
       }
	   
	   val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)
	   
	   val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
	   val view = binding.root
	   
	   binding.message.visibility = View.VISIBLE
	  
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.git_clone_in_progress)
       builder.setMessage(url)
       builder.setView(view)
       builder.setCancelable(false)
	   
	   val repoName = url.substringAfterLast('/').substringBeforeLast(".git")
		   
	   val targetDir:File
	   
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
	   targetDir = File(ApplicationLoader.applicationContext.getExternalFilesDir("/Projects"), repoName)
       } else {  
	   targetDir = File(Environment.getExternalStorageDirectory().absolutePath + "/CodeAssistProjects"  , repoName)
       }
	   
	   val progress = GitCloneProgressMonitor(binding.progress, binding.message)
       var git: Git? = null
	    
	   val future = 
	   if(url.startsWith("git@github.com")) {
	   executeAsyncProvideError(
	   {
	   return@executeAsyncProvideError Git.cloneRepository()
	   .setURI(url)
	   .setDirectory(targetDir)
	   .setTransportConfigCallback(sshTransportConfigCallback)
	   .setProgressMonitor(progress)
	   .call()
	   .also { git = it }
	   },
	   { _, _ -> }
	   )
	   } else
	   executeAsyncProvideError(
	   {
	   return@executeAsyncProvideError Git.cloneRepository()
	   .setURI(url)
	   .setDirectory(targetDir)
	   .setProgressMonitor(progress)
	   .call()
	   .also { git = it }
	   },
	   { _, _ -> }
	   )
	   
	   builder.setPositiveButton(android.R.string.cancel) { iface, _ ->
       iface.dismiss()
       progress.cancel()
       git?.close()
       future.cancel(true)
       }
       
 	  val dialog = builder.show() 
	   
	   future.whenComplete { result, error ->
       ThreadUtils.runOnUiThread {
       dialog?.dismiss()
     
       if (result == null || error != null) {
       if (!future.isCancelled) {
	   showCloneError(error, context)
       }
       } else {
       
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.success)
	   builder.setMessage(url+" " + context.getString(R.string.cloned_successfully))
       builder.setPositiveButton(android.R.string.ok, null)
       builder.show()
	   }
       }
       }
	   }
	   
	   private fun showCloneError(error: Throwable, context:Context) {
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setTitle(R.string.git_clone_failed)
       builder.setMessage(error.localizedMessage)
       builder.setPositiveButton(android.R.string.ok, null)
       builder.show()
	   }
	   
	   class GitCloneProgressMonitor(val progress: LinearProgressIndicator, val message: TextView) :
       ProgressMonitor {

       private var cancelled = false

       fun cancel() {
       cancelled = true
       }

       override fun start(totalTasks: Int) {
       ThreadUtils.runOnUiThread { progress.max = totalTasks }
       }
  
       override fun beginTask(title: String?, totalWork: Int) {
       ThreadUtils.runOnUiThread { message.text = title }
       }

       override fun update(completed: Int) {
       ThreadUtils.runOnUiThread { progress.progress = completed }
       }

       override fun endTask() {}

       override fun isCancelled(): Boolean {
       return cancelled || Thread.currentThread().isInterrupted
       }
       }
       fun gitActions(context:Context, project:Project) {
       val option = arrayOf(
            "Init",	//0		
            "Status", //1
			"Fetch", //2
			"Add all to stage", //3
		    "Commit", //4
		    "New Branch", //5
	        "Delete Branch", //6
		    "Add Remote", //7
	        "Remove Remote", //8
            "Push", //9
            "Pull", //10
            "Reset (HARD)", //11
            )
					
	   val builder = MaterialAlertDialogBuilder(context)
       
	   builder.setItems(option) { _, which ->
	 
	   when (which) {
		 0 -> { val future =
	   executeAsyncProvideError(
	   {
		var git: Git? = null
		var file = File(project.getRootFile(), "/.git")
		var path = file.toString()
		if (file.exists()){
		git = Git.init().setDirectory(project.getRootFile()).call()
		ThreadUtils.runOnUiThread {
		Toast.makeText(context,"Reinitialized existing Git repository in " + path,Toast.LENGTH_SHORT).show()  
	  }
		} else {
		git = Git.init().setDirectory(project.getRootFile()).call()
		ThreadUtils.runOnUiThread {
		Toast.makeText(context,"Initialized empty Git repository in " + path,Toast.LENGTH_SHORT).show()  
	  }
		}
	  // .also { git = it }
	   
	   return@executeAsyncProvideError
	   },
	   { _, _ -> }   
	   )  
	   
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       } else {
       
        }   
      }  }
       }     
	       1 -> { 
	       
	val future =
	   executeAsyncProvideError(
	   {
		 var git: Git? = null
		
		
		var status: Status =    Git.open(project.getRootFile()).status().call()
	  	 convertStatus(status)
	  	 
		 ThreadUtils.runOnUiThread {
	 	 val builder = MaterialAlertDialogBuilder(context)      
         builder.setMessage(mResult)	   
         builder.show()
	   }	 	   
	   return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } }
	       
	     }
	     
	          2 -> { 
   
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)
	   
	   val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
	   val view = binding.root
	   
	   binding.message.visibility = View.VISIBLE
	  
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Fetching...")
       builder.setView(view)
       builder.setCancelable(false)
      
   	          
	  
	  val progress = GitCloneProgressMonitor(binding.progress, binding.message)

	  val future =
	   executeAsyncProvideError(
	   {
		 var git: Git? = null
		 
		val remote:FetchResult =    Git.open(project.getRootFile()).fetch().setTransportConfigCallback(sshTransportConfigCallback) .setProgressMonitor(progress).call()
	  	 
	   return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   val dialog = builder.show() 
	         
	   
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	   dialog?.dismiss()
	   
       if (result == null || error != null) {
	   showError(error, context)
       }   else {
       		Toast.makeText(context,"Fetch completed",Toast.LENGTH_SHORT).show()  
        }     
       }}        
	     }
	  
	     
	            3 -> { 
	       
	val future =
	   executeAsyncProvideError(
	   {
		
	     Git.open(project.getRootFile()).add().addFilepattern(".").call()
		 	 
		 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"File has been added to stage",Toast.LENGTH_SHORT).show()  

	   }	 	   
	   return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } }
	       
	     }
	     
	               4 -> { 
	            
	               
	         	 val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint("Commit message")
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Commit Changes")
	   builder.setView(binding.root)
	   
       
  
       builder.setPositiveButton("Commit") { dialog, _ ->
       
       val userName  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_NAME,"")  
       val userEmail  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_EMAIL,"")  

         
       val future =
	   executeAsyncProvideError(
	   {
	    
	   val msg = binding.textinputLayout.editText?.text?.toString()
       
	       
	    var file = File(project.getRootFile(), "/.git")
		var path = file.toString()
		
		if (userName.isNullOrBlank() && userEmail.isNullOrBlank()) {
        ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Please set Username and Email in settings",Toast.LENGTH_SHORT).show()  
	   }	 	   
    } else if (msg.isNullOrBlank()) {
    ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Commit message can't be empty",Toast.LENGTH_SHORT).show()  
	   }	 	   
    }
    
    else {
    
      Git.open(project.getRootFile()).commit().setAll(true).setCommitter(userName.toString(), userEmail.toString()) .setMessage(msg) .call()
	   
               	 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Committed all changes to repository in " + path,Toast.LENGTH_SHORT).show()  
	   }	 	   

        
    }
		
	   	   
	   return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } }
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
       builder.show()
	                
	     }
	     
	              5 -> { 
	       

        	 val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint("Branch name")
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Create new branch")
	   builder.setView(binding.root)
	   builder.setPositiveButton("Create") { dialog, _ ->
         
       val future =
	   executeAsyncProvideError(
	   {
	   val branch = binding.textinputLayout.editText?.text?.toString()
 
	    if (branch.isNullOrBlank()) {
    ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Branch name can't be empty",Toast.LENGTH_SHORT).show()  
	   }	 	   
    } else {
    
    Git.open(project.getRootFile()).checkout().setName(branch).setCreateBranch(true) .call()
	   
               	 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"New branch created successfully",Toast.LENGTH_SHORT).show()  
	   }	 	   

    }
	   
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } } 
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
		 builder.show()	 
	     }
	   
	             6 -> { 
	       

        	 val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint("Branch name")
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Delete branch")
	   builder.setView(binding.root)
	   builder.setPositiveButton("Delete") { dialog, _ ->
         
       val future =
	   executeAsyncProvideError(
	   {
	   val branch = binding.textinputLayout.editText?.text?.toString()
 
	    if (branch.isNullOrBlank()) {
    ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Branch name can't be empty",Toast.LENGTH_SHORT).show()  
	   }	 	   
    } else {
    
    Git.open(project.getRootFile()).branchDelete()
                                                    .setBranchNames(branch)
                                                    .setForce(true)
                                                    .call()
	   
               	 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Branch deleted successfully",Toast.LENGTH_SHORT).show()  
	   }	 	   

    }
	   
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } } 
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
		 builder.show()	 
	     }
	   
	   
	   
	                  7 -> { 
	       

        	 val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint("Remote name")
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Add Remote")
	   builder.setView(binding.root)
	   builder.setPositiveButton("Add") { dialog, _ ->
         
       val future =
	   executeAsyncProvideError(
	   {
	   
	   val remote = binding.textinputLayout.editText?.text?.toString()
	   
	    if (remote.isNullOrBlank()) {
    ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Remote name can't be empty",Toast.LENGTH_SHORT).show()  
	   }	 	   
    } else {
    val userName  = sharedPreferences.getString(SharedPreferenceKeys.GIT_USER_NAME,"")  
       
    
       val url : String = "git@github.com:"+ userName.toString()+"/" + project.getRootFile().getName()+ ".git"
  	   val  config : StoredConfig = Git.open(project.getRootFile()).getRepository().getConfig()
       config.setString("remote", remote, "url", url)
       config.setString("remote", remote, "fetch", "+refs/heads/*:refs/remotes/" + remote +"/*");
       config.save()
       
               	 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Remote added successfully",Toast.LENGTH_SHORT).show()  
	   }	 	   

    }
	   
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } } 
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
		 builder.show()	 
	     }
	     
	                       8 -> { 
	       

        	 val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.base_textinput_layout, null)
	   
	   val binding = BaseTextinputLayoutBinding.inflate(inflater,null,false)
	   binding.textinputLayout.setHint("Remote name")
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Remove Remote")
	   builder.setView(binding.root)
	   builder.setPositiveButton("Remove") { dialog, _ ->
         
       val future =
	   executeAsyncProvideError(
	   {
	   
	   val remote = binding.textinputLayout.editText?.text?.toString()
	   
	    if (remote.isNullOrBlank()) {
    ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Remote name can't be empty",Toast.LENGTH_SHORT).show()  
	   }	 	   
    } else {

  	   val  config : StoredConfig = Git.open(project.getRootFile()).getRepository().getConfig()
       config.unsetSection("remote", remote);
       config.save()
       
               	 ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Remote removed successfully",Toast.LENGTH_SHORT).show()  
	   }	 	   

    }
	   
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } } 
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
		 builder.show()	 
	     }
	     
	                  9 -> { 
	                  
	     val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)  
	   val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
	   val view = binding.root   
	   binding.message.visibility = View.VISIBLE 
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Pushing...")
       builder.setView(view)
       builder.setCancelable(false)     
	  
	  val progress = GitCloneProgressMonitor(binding.progress, binding.message)
             
	               
       val future =
	   executeAsyncProvideError(
	   {
	    
	   Git.open(project.getRootFile()).push().setProgressMonitor(progress).setTransportConfigCallback(sshTransportConfigCallback).call()
	
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )  
	      val dialog = builder.show() 
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	   dialog?.dismiss()
	   
       if (result == null || error != null) {
	   showError(error, context)
       }   else {
       		Toast.makeText(context,"Push completed",Toast.LENGTH_SHORT).show()  
        }     
       }}        
	     }	     
	                  10 -> { 
	
       val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater        
       inflater.inflate(R.layout.layout_dialog_progress, null)  
	   val binding = LayoutDialogProgressBinding.inflate(inflater,null,false)
	   val view = binding.root   
	   binding.message.visibility = View.VISIBLE 
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Pulling...")
       builder.setView(view)
       builder.setCancelable(false)     
	  
	  val progress = GitCloneProgressMonitor(binding.progress, binding.message)
            	              
       val future =
	   executeAsyncProvideError(
	   {
	
	 Git.open(project.getRootFile()).pull().setProgressMonitor(progress).setTransportConfigCallback(sshTransportConfigCallback).call()
	
		 	return@executeAsyncProvideError
	   
	   },
	   { _, _ -> }   
	   )
	     
	      val dialog = builder.show() 
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	   dialog?.dismiss()
	   
       if (result == null || error != null) {
	   showError(error, context)
       }   else {
       		Toast.makeText(context,"Pull completed",Toast.LENGTH_SHORT).show()  
        }     
       }}        
	     }
	     
	                  11 -> { 
	       
	   val builder = MaterialAlertDialogBuilder(context)
	   builder.setTitle("Reset Changes")
	   builder.setMessage("Are you sure you want to discard all the changes?")
	   builder.setPositiveButton("Reset") { dialog, _ ->
         
       val future =
	   executeAsyncProvideError(
	   {
	   
	   Git.open(project.getRootFile()).reset().setMode(ResetCommand.ResetType.HARD).call()
	   
		ThreadUtils.runOnUiThread {
	 	 Toast.makeText(context,"Changes has been discarded",Toast.LENGTH_SHORT).show()  
	 
	   }	 	   
		
		 	return@executeAsyncProvideError
	   
	   
	   },
	   { _, _ -> }   
	   )  
	   future.whenComplete { result, error ->
	   ThreadUtils.runOnUiThread {
	  
       if (result == null || error != null) {
	   showError(error, context)
       }    } } 
       
       }
       builder.setNegativeButton(android.R.string.cancel, null)
		 builder.show()	 
	     }
	     
	     
	       }   }
	   builder.show()
	   }
	   private fun showError(error: Throwable, context:Context) {
	   val builder = MaterialAlertDialogBuilder(context)
       builder.setMessage(error.localizedMessage)
       builder.show()
	   }
	   	   private fun convertStatus(status:Status) {
        if (!status.hasUncommittedChanges() && status.isClean()) {
         mResult.setLength(0)
            mResult.append("Nothing to commit, working directory clean")
            return
        }
        mResult.setLength(0)
        convertStatusSet("Added files:", status.getAdded())
        convertStatusSet("Changed files:", status.getChanged())
        convertStatusSet("Removed files:", status.getRemoved())
        convertStatusSet("Missing files:", status.getMissing())
        convertStatusSet("Modified files:", status.getModified())
        convertStatusSet("Conflicting files:", status.getConflicting())
        convertStatusSet("Untracked files:", status.getUntracked())
        
    }
	   
    private fun convertStatusSet(str:String, set:Set<String>) {
   
	    if (!set.isEmpty()) {
            mResult.append(str)
            mResult.append("\n\n")
		for (s in set) {
		
            mResult.append("\t");
            mResult.append(s)
            mResult.append("\n")
			  }	
            mResult.append("\n")
            
			 }
			  }

	   }
	   
