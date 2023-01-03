package com.tyron.code.tasks.git

import android.widget.TextView
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.eclipse.jgit.lib.ProgressMonitor


class GitProgressMonitor(val progress: LinearProgressIndicator, val message: TextView) :
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
