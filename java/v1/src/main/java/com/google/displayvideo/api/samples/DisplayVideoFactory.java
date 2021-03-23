// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.displayvideo.api.samples;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.displayvideo.v1.DisplayVideo;
import com.google.api.services.displayvideo.v1.DisplayVideoScopes;
import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Factory for DisplayVideo clients that handles OAuth and service creation for all Display &amp;
 * Video 360 API samples.
 */
public final class DisplayVideoFactory {
  /** Directory to store user credentials. */
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".store/dv360_sample");

  /** Default location for client secrets file. */
  private static final String CLIENT_SECRETS_FILE = "/client_secrets.json";

  /** HTTP read timeout for DBM API requests (in ms). Defaults to 3 minutes. * */
  private static final int HTTP_READ_TIMEOUT_IN_MILLIS = 3 * 60_000;

  /** HTTP connect timeout for DBM API requests (in ms). Defaults to 3 minutes. * */
  private static final int HTTP_CONNECT_TIMEOUT_IN_MILLIS = 3 * 60_000;

  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or
   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "";

  private static final HttpTransport HTTP_TRANSPORT = Utils.getDefaultTransport();
  private static final JsonFactory JSON_FACTORY = Utils.getDefaultJsonFactory();

  /**
   * Authorizes the installed application to access user's protected data.
   *
   * @return A {@link Credential} object initialized with the current user's credentials.
   */
  private static Credential authorize(String clientSecretsFile) throws Exception {
    // Load application default credentials if they're available.
    Credential credential = loadApplicationDefaultCredentials();

    // Otherwise, load credentials from the set client secrets file or a provided client secrets
    // file.
    if (credential == null) {
      if (clientSecretsFile == null) {
        if (DisplayVideoFactory.class.getResource(CLIENT_SECRETS_FILE) != null) {
          clientSecretsFile = DisplayVideoFactory.class.getResource(CLIENT_SECRETS_FILE).getFile();
        } else {
          Console console = System.console();
          console.printf(
              "A client secrets file was not provided and the default client secrets file could"
                  + " not be found in the resources folder.%n");
          console.printf("Please provide the path to a client secrets JSON file.%n");
          console.printf("Enter path to client secrets file:%n");
          clientSecretsFile = console.readLine();
        }
      }

      credential = loadUserCredentials(clientSecretsFile, new FileDataStoreFactory(DATA_STORE_DIR));
    }

    return credential;
  }

  /**
   * Attempts to load application default credentials.
   *
   * @return A {@link Credential} object initialized with application default credentials, or {@code
   *     null} if none were found.
   */
  private static Credential loadApplicationDefaultCredentials() {
    try {
      GoogleCredential credential = GoogleCredential.getApplicationDefault();
      return credential.createScoped(Collections.singleton(DisplayVideoScopes.DISPLAY_VIDEO));
    } catch (IOException ignored) {
      // No application default credentials, continue to try other options.
    }

    return null;
  }

  /**
   * Attempts to load user credentials from the provided client secrets file and persists data to
   * the provided data store.
   *
   * @param clientSecretsFile The path to the file containing client secrets.
   * @param dataStoreFactory he data store to use for caching credential information.
   * @return A {@link Credential} object initialized with user account credentials.
   */
  private static Credential loadUserCredentials(
      String clientSecretsFile, DataStoreFactory dataStoreFactory) throws Exception {
    // Load client secrets JSON file.
    GoogleClientSecrets clientSecrets;
    try (Reader reader = Files.newBufferedReader(Paths.get(clientSecretsFile), UTF_8)) {
      clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
    }

    // Set up the authorization code flow.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(DisplayVideoScopes.DISPLAY_VIDEO))
            .setDataStoreFactory(dataStoreFactory)
            .build();

    // Authorize and persist credential information to the data store.
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  /**
   * Adjusts HTTP timeout values used by the provided request initializer.
   *
   * @param initializer The {@link HttpRequestInitializer} used to authorize requests.
   * @return An {@link HttpRequestInitializer} with modified HTTP timeout values.
   */
  private static HttpRequestInitializer setHttpTimeout(
      final HttpRequestInitializer requestInitializer) {
    return new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        requestInitializer.initialize(httpRequest);
        httpRequest.setConnectTimeout(HTTP_CONNECT_TIMEOUT_IN_MILLIS);
        httpRequest.setReadTimeout(HTTP_READ_TIMEOUT_IN_MILLIS);
      }
    };
  }

  /**
   * Performs all necessary setup steps for running requests against the API.
   *
   * @return An initialized {@link DisplayVideo} service object.
   */
  public static DisplayVideo getInstance(String clientSecretsFile) throws Exception {
    Credential credential = authorize(clientSecretsFile);

    String modifiedApplicationName = APPLICATION_NAME;
    if (APPLICATION_NAME != null && !APPLICATION_NAME.trim().isEmpty()) {
      modifiedApplicationName += "_JavaSamples";
    }

    // Create DisplayVideo service object.
    DisplayVideo displayVideo =
        new DisplayVideo.Builder(HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(credential))
            .setApplicationName(modifiedApplicationName)
            .build();

    return displayVideo;
  }
}
