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

package com.netflix.spinnaker.orca.coveo.tasks

import com.netflix.spinnaker.orca.api.pipeline.TaskResult
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.tasks.manifest.PromoteManifestKatoOutputsTask
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import org.springframework.stereotype.Component

private val OUTPUTS_TO_COPY =
  listOf(
    "artifacts",
    "outputs.boundArtifacts",
    "outputs.createdArtifacts",
    "outputs.manifests",
    "outputs.manifestNamesByNamespace"
  )

@Component
class PromoteInnerOutputsTask : InnerJobAware, PromoteManifestKatoOutputsTask() {
  override fun execute(stage: StageExecution): TaskResult {
    val taskResult = super.execute(stage)
    val jobOutputs = getSelectedOutputs(taskResult, OUTPUTS_TO_COPY)

    return TaskResult.builder(ExecutionStatus.SUCCEEDED)
      .outputs(getUpdatedInnerJobOutputs(stage, jobOutputs))
      .build()
  }
}
