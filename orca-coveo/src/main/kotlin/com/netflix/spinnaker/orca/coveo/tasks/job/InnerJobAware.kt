/*
 * Copyright 2021 Coveo Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.coveo.tasks.job

import com.netflix.spinnaker.orca.api.pipeline.TaskResult
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.ext.mapTo

interface InnerJobAware {
  fun getCurrentInnerJobKey(stage: StageExecution): String {
    return stage.context["currentInnerJob"] as? String
      ?: throw IllegalStateException("currentInnerStage must be set")
  }

  fun getInnerJobContext(stage: StageExecution, innerJobKey: String): Map<String, Any?>? {
    return stage.context[innerJobKey] as? Map<String, Any?>?
  }

  fun getInnerJobContext(stage: StageExecution, innerJob: InnerJob): Map<String, Any?>? {
    return stage.context[innerJob.innerJobKey] as? Map<String, Any?>?
  }

  fun getCurrentInnerJobContext(stage: StageExecution): Map<String, Any?> {
    return getInnerJobContext(stage, getCurrentInnerJobKey(stage)) ?: mapOf()
  }

  fun getInnerJobOutputs(stage: StageExecution, innerJobKey: String): Map<String, Any?>? {
    return stage.outputs[innerJobKey] as? Map<String, Any?>?
  }

  fun getCurrentInnerJobOutputs(stage: StageExecution): Map<String, Any?> {
    return getInnerJobOutputs(stage, getCurrentInnerJobKey(stage)) ?: mapOf()
  }

  fun getUpdatedContextWithInnerJob(
    stage: StageExecution,
    updatedInnerJobContext: Map<String, Any?>
  ): Map<String, Any?> {
    return mapOf(
      getCurrentInnerJobKey(stage) to (getCurrentInnerJobContext(stage) + updatedInnerJobContext)
    )
  }

  fun getUpdatedOutputsWithInnerJob(
    stage: StageExecution,
    updatedInnerJobOutputs: Map<String, Any?>
  ): Map<String, Any?> {
    return mapOf(
      getCurrentInnerJobKey(stage) to (getCurrentInnerJobOutputs(stage) + updatedInnerJobOutputs)
    )
  }

  fun getContextToCopy(taskResult: TaskResult, keys: Set<String>): Map<String, Any?> {
    return taskResult.context.filterKeys { keys.contains(it) }
  }

  fun getOutputsToCopy(taskResult: TaskResult, keys: Set<String>): Map<String, Any?> {
    return taskResult.outputs.filterKeys { keys.contains(it) }
  }
}

inline fun <reified O> InnerJobAware.mapInnerJobTo(stage: StageExecution): O {
  /** Use the JSON Pointer Syntax */
  return stage.mapTo("/${getCurrentInnerJobKey(stage)}")
}
