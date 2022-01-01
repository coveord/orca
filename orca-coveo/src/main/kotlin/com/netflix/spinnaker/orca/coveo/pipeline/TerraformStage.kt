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

package com.netflix.spinnaker.orca.coveo.pipeline

import com.netflix.spinnaker.orca.api.pipeline.CancellableStage
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.tasks.job.DestroyJobTask
import com.netflix.spinnaker.orca.coveo.tasks.*
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJob
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import com.netflix.spinnaker.orca.coveo.tasks.job.TerraformStageInnerJob
import com.netflix.spinnaker.orca.ext.withTask
import org.springframework.stereotype.Component

private val INNER_KEYS_TO_CLEAR_ON_RESTART =
  setOf("completionDetails", "deploy.jobs", "jobStatus", "propertyFileContents")

@Component
class TerraformStage(destroyJobTask: DestroyJobTask) : StageDefinitionBuilder, CancellableStage, InnerJobAware {
  override fun taskGraph(stage: StageExecution, builder: TaskNode.Builder) {
    stage.context["currentInnerJob"] = TerraformStageInnerJob.PLAN.key

    // The empty tasks are workaround since Orca doesn't support loops that aren't surrounded with tasks
    builder.withTask<EmptyTask>("emptyTask")
    builder.withLoop { subGraph ->
      subGraph
        .withTask<RunInnerJobTask>("runInnerJob")
        .withTask<MonitorInnerJobTask>("monitorJob")
        .withTask<PromoteInnerOutputsTask>("promoteInnerOutputs")
        .withTask<WaitOnInnerJobCompletionTask>("waitOnInnerJobCompletion")
        .withTask<BindInnerProducedArtifactsTask>("bindInnerProducedArtifacts")
        .withTask<ConsumeInnerArtifactTask>("consumeInnerArtifact")
        .withTask<HandleInnerJobResultAndApprovalTask>("handleInnerJobResultAndApproval")
    }
    builder.withTask<EmptyTask>("emptyTask")
  }

  private fun prepareInnerJobForRestart(stage: StageExecution, innerJob: InnerJob) {
    val clearedContext =
      getInnerJobContext(stage, innerJob)?.filterKeys {
        !INNER_KEYS_TO_CLEAR_ON_RESTART.contains(it)
      }

    clearedContext?.let { stage.context[innerJob.innerJobKey] = it }
  }

  override fun prepareStageForRestart(stage: StageExecution) {
    TerraformStageInnerJob.values().forEach { prepareInnerJobForRestart(stage, it) }

    stage.context.remove("judgmentStatus")
    stage.context.remove("lastModifiedBy")
    stage.context["currentInnerJob"] = TerraformStageInnerJob.PLAN.key
  }

  override fun cancel(stage: StageExecution): CancellableStage.Result {
    TODO("Not yet implemented")
  }
}
