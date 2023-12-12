/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.service.rest;

public class RestConstants {

    private RestConstants() {
    }

    public static final String REST_PROP_SERVICES_URL = "water.rest.services.url";
    public static final String REST_PROP_FRONTEND_URL = "water.rest.frontend.url";
    public static final String REST_PROP_ROOT_CONTEXT = "water.rest.root.context";
    public static final String REST_PROP_UPLOAD_PATH = "water.rest.uploadFolder.path";
    public static final String REST_PROP_UPLOAD_MAX_FILE_SIZE = "water.rest.uploadFolder.maxFileSize";
}
