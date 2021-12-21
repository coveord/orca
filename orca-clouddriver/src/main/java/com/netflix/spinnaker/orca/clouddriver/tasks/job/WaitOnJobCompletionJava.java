/*
 * Copyright 2021 Coveo Solutions inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
 *
 */

package com.netflix.spinnaker.orca.clouddriver.tasks.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.kork.core.RetrySupport;
import com.netflix.spinnaker.orca.clouddriver.KatoRestService;
import com.netflix.spinnaker.orca.front50.Front50Service;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import groovy.lang.MetaClass;
import org.jetbrains.annotations.Nullable;

public class WaitOnJobCompletionJava extends WaitOnJobCompletion {
  public WaitOnJobCompletionJava(
      KatoRestService katoRestService,
      ObjectMapper objectMapper,
      RetrySupport retrySupport,
      JobUtils jobUtils,
      @Nullable Front50Service front50Service,
      ExecutionRepository repository) {
    super(katoRestService, objectMapper, retrySupport, jobUtils, front50Service, repository);
  }

  // Workaround since Kotlin compiler can't recognize Groovy synthetic methods in super classes
  // See: https://youtrack.jetbrains.com/issue/KT-27998
  @Override
  public Object invokeMethod(String s, Object o) {
    return super.invokeMethod(s, o);
  }

  @Override
  public Object getProperty(String s) {
    return super.getProperty(s);
  }

  @Override
  public void setProperty(String s, Object o) {
    super.setProperty(s, o);
  }

  @Override
  public MetaClass getMetaClass() {
    return super.getMetaClass();
  }

  @Override
  public void setMetaClass(MetaClass metaClass) {
    super.setMetaClass(metaClass);
  }
}
