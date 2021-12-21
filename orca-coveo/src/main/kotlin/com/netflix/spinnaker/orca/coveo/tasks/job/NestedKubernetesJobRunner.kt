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

import com.fasterxml.jackson.module.kotlin.convertValue
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.tasks.job.JobRunner
import com.netflix.spinnaker.orca.clouddriver.tasks.manifest.ManifestEvaluator
import com.netflix.spinnaker.orca.clouddriver.tasks.manifest.RunJobManifestContext
import com.netflix.spinnaker.orca.clouddriver.tasks.providers.kubernetes.KubernetesContainerFinder
import com.netflix.spinnaker.orca.clouddriver.tasks.providers.kubernetes.Manifest
import com.netflix.spinnaker.orca.clouddriver.tasks.providers.kubernetes.ManifestAnnotationExtractor
import com.netflix.spinnaker.orca.jackson.OrcaObjectMapper
import com.netflix.spinnaker.orca.pipeline.util.ArtifactUtils
import org.springframework.stereotype.Component

@Component
class NestedKubernetesJobRunner(
  private val artifactUtils: ArtifactUtils,
  private val manifestEvaluator: ManifestEvaluator
) : InnerJobAware, JobRunner {
  override fun getOperations(stage: StageExecution): List<Map<String, Map<Any?, Any?>?>> {
    val result = evaluateManifest(stage)
    val manifests = result.manifests
    if (manifests.size > 1) {
      throw IllegalArgumentException("Run Job only supports manifests with a single Job.")
    }

    val operation =
      mutableMapOf(
        "account" to stage.context["account"],
        "credentials" to stage.context["credentials"],
        "application" to stage.context["application"],
        "source" to "text",
        "manifest" to manifests.first(),
        "requiredArtifacts" to result.requiredArtifacts,
        "optionalArtifacts" to result.optionalArtifacts,
      )
    KubernetesContainerFinder.populateFromStage(operation, stage, artifactUtils)

    return listOf(mapOf(JobRunner.OPERATION to operation.toMap()))
  }

  private fun evaluateManifest(stage: StageExecution): ManifestEvaluator.Result {
    val manifestContext = mapInnerJobTo<RunJobManifestContext>(stage)
    return manifestEvaluator.evaluate(stage, manifestContext)
  }

  override fun getAdditionalOutputs(
    stage: StageExecution,
    operations: List<Map<*, *>>
  ): Map<String, Any> {
    val operation = operations.first()[JobRunner.OPERATION] as? Map<*, *> ?: return mapOf()
    val manifest =
      operation["manifest"]?.let { rawManifest ->
        OrcaObjectMapper.getInstance().convertValue<Manifest>(rawManifest)
      }

    val logTemplate = manifest?.let { ManifestAnnotationExtractor.logs(it) }
    logTemplate?.let {
      return mapOf("execution" to mapOf("logs" to it))
    }
    return mapOf()
  }

  override fun isKatoResultExpected() = false
  override fun getCloudProvider() = "nestedKubernetes"
}
