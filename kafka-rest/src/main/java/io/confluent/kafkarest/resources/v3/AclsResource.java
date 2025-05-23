/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.kafkarest.resources.v3;

import static java.util.Objects.requireNonNull;

import io.confluent.kafkarest.Errors;
import io.confluent.kafkarest.controllers.AclManager;
import io.confluent.kafkarest.entities.Acl;
import io.confluent.kafkarest.entities.Acl.Operation;
import io.confluent.kafkarest.entities.Acl.PatternType;
import io.confluent.kafkarest.entities.Acl.Permission;
import io.confluent.kafkarest.entities.Acl.ResourceType;
import io.confluent.kafkarest.entities.v3.AclData;
import io.confluent.kafkarest.entities.v3.AclDataList;
import io.confluent.kafkarest.entities.v3.CreateAclRequest;
import io.confluent.kafkarest.entities.v3.DeleteAclsResponse;
import io.confluent.kafkarest.entities.v3.Resource;
import io.confluent.kafkarest.entities.v3.ResourceCollection;
import io.confluent.kafkarest.entities.v3.SearchAclsResponse;
import io.confluent.kafkarest.extension.ResourceAccesslistFeature.ResourceName;
import io.confluent.kafkarest.resources.AsyncResponses;
import io.confluent.kafkarest.resources.AsyncResponses.AsyncResponseBuilder;
import io.confluent.kafkarest.response.UrlFactory;
import io.confluent.rest.annotations.PerformanceMetric;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Path("/v3/clusters/{clusterId}/acls")
@ResourceName("api.v3.acls.*")
public final class AclsResource {

  private final Provider<AclManager> aclManager;
  private final UrlFactory urlFactory;

  @Inject
  public AclsResource(Provider<AclManager> aclManager, UrlFactory urlFactory) {
    this.aclManager = requireNonNull(aclManager);
    this.urlFactory = requireNonNull(urlFactory);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PerformanceMetric("v3.acls.list")
  @ResourceName("api.v3.acls.list")
  public void searchAcls(
      @Suspended AsyncResponse asyncResponse,
      @PathParam("clusterId") String clusterId,
      @QueryParam("resource_type") @DefaultValue("any") ResourceType resourceType,
      @QueryParam("resource_name") @DefaultValue("") String resourceName,
      @QueryParam("pattern_type") @DefaultValue("any") PatternType patternType,
      @QueryParam("principal") @DefaultValue("") String principal,
      @QueryParam("host") @DefaultValue("") String host,
      @QueryParam("operation") @DefaultValue("any") Operation operation,
      @QueryParam("permission") @DefaultValue("any") Permission permission) {
    if (resourceType == Acl.ResourceType.UNKNOWN) {
      throw new BadRequestException("resource_type cannot be UNKNOWN");
    }
    if (patternType == Acl.PatternType.UNKNOWN) {
      throw new BadRequestException("pattern_type cannot be UNKNOWN");
    }
    if (operation == Acl.Operation.UNKNOWN) {
      throw new BadRequestException("operation cannot be UNKNOWN");
    }
    if (permission == Acl.Permission.UNKNOWN) {
      throw new BadRequestException("permission cannot be UNKNOWN");
    }

    CompletableFuture<SearchAclsResponse> response =
        aclManager
            .get()
            .searchAcls(
                clusterId,
                resourceType,
                resourceName.isEmpty() ? null : resourceName,
                patternType,
                principal.isEmpty() ? null : principal,
                host.isEmpty() ? null : host,
                operation,
                permission)
            .thenApply(
                acls ->
                    SearchAclsResponse.create(
                        AclDataList.builder()
                            .setMetadata(
                                ResourceCollection.Metadata.builder()
                                    .setSelf(
                                        urlFactory
                                            .newUrlBuilder()
                                            .appendPathSegment("v3")
                                            .appendPathSegment("clusters")
                                            .appendPathSegment(clusterId)
                                            .appendPathSegment("acls")
                                            .putQueryParameter("resource_type", resourceType.name())
                                            .putQueryParameter("resource_name", resourceName)
                                            .putQueryParameter("pattern_type", patternType.name())
                                            .putQueryParameter("principal", principal)
                                            .putQueryParameter("host", host)
                                            .putQueryParameter("operation", operation.name())
                                            .putQueryParameter("permission", permission.name())
                                            .build())
                                    .build())
                            .setData(
                                acls.stream()
                                    .map(this::toAclData)
                                    .sorted(
                                        Comparator.comparing(AclData::getResourceType)
                                            .thenComparing(AclData::getResourceName)
                                            .thenComparing(AclData::getPatternType)
                                            .thenComparing(AclData::getPrincipal)
                                            .thenComparing(AclData::getHost)
                                            .thenComparing(AclData::getOperation)
                                            .thenComparing(AclData::getPermission))
                                    .collect(Collectors.toList()))
                            .build()));

    AsyncResponses.asyncResume(asyncResponse, response);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @PerformanceMetric("v3.acls.create")
  @ResourceName("api.v3.acls.create")
  public void createAcl(
      @Suspended AsyncResponse asyncResponse,
      @PathParam("clusterId") String clusterId,
      @Valid CreateAclRequest request) {

    if (request == null) {
      throw Errors.invalidPayloadException(Errors.NULL_PAYLOAD_ERROR_MESSAGE);
    }

    CompletableFuture<Void> response =
        aclManager
            .get()
            .validateAclCreateParameters(Collections.singletonList(request))
            .createAcl(
                clusterId,
                request.getResourceType(),
                request.getResourceName(),
                request.getPatternType(),
                request.getPrincipal(),
                request.getHost(),
                request.getOperation(),
                request.getPermission());

    AsyncResponseBuilder.from(
            Response.status(Status.CREATED)
                .location(
                    URI.create(
                        urlFactory
                            .newUrlBuilder()
                            .appendPathSegment("v3")
                            .appendPathSegment("clusters")
                            .appendPathSegment(clusterId)
                            .appendPathSegment("acls")
                            .putQueryParameter("resource_type", request.getResourceType().name())
                            .putQueryParameter("resource_name", request.getResourceName())
                            .putQueryParameter("pattern_type", request.getPatternType().name())
                            .putQueryParameter("principal", request.getPrincipal())
                            .putQueryParameter("host", request.getHost())
                            .putQueryParameter("operation", request.getOperation().name())
                            .putQueryParameter("permission", request.getPermission().name())
                            .build())))
        .entity(response)
        .asyncResume(asyncResponse);
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @PerformanceMetric("v3.acls.delete")
  @ResourceName("api.v3.acls.delete")
  public void deleteAcls(
      @Suspended AsyncResponse asyncResponse,
      @PathParam("clusterId") String clusterId,
      @QueryParam("resource_type") @DefaultValue("unknown") ResourceType resourceType,
      @QueryParam("resource_name") @DefaultValue("") String resourceName,
      @QueryParam("pattern_type") @DefaultValue("unknown") PatternType patternType,
      @QueryParam("principal") @DefaultValue("") String principal,
      @QueryParam("host") @DefaultValue("") String host,
      @QueryParam("operation") @DefaultValue("unknown") Operation operation,
      @QueryParam("permission") @DefaultValue("unknown") Permission permission) {
    if (resourceType == Acl.ResourceType.UNKNOWN) {
      throw new BadRequestException("resource_type cannot be unspecified or UNKNOWN");
    }
    if (patternType == Acl.PatternType.UNKNOWN) {
      throw new BadRequestException("pattern_type cannot be unspecified or UNKNOWN");
    }
    if (operation == Acl.Operation.UNKNOWN) {
      throw new BadRequestException("operation cannot be unspecified or UNKNOWN");
    }
    if (permission == Acl.Permission.UNKNOWN) {
      throw new BadRequestException("permission cannot be unspecified or UNKNOWN");
    }

    CompletableFuture<DeleteAclsResponse> response =
        aclManager
            .get()
            .deleteAcls(
                clusterId,
                resourceType,
                resourceName.isEmpty() ? null : resourceName,
                patternType,
                principal.isEmpty() ? null : principal,
                host.isEmpty() ? null : host,
                operation,
                permission)
            .thenApply(
                acls ->
                    DeleteAclsResponse.create(
                        acls.stream()
                            .map(this::toAclData)
                            .sorted(
                                Comparator.comparing(AclData::getResourceType)
                                    .thenComparing(AclData::getResourceName)
                                    .thenComparing(AclData::getPatternType)
                                    .thenComparing(AclData::getPrincipal)
                                    .thenComparing(AclData::getHost)
                                    .thenComparing(AclData::getOperation)
                                    .thenComparing(AclData::getPermission))
                            .collect(Collectors.toList())));

    AsyncResponses.asyncResume(asyncResponse, response);
  }

  public AclData toAclData(Acl acl) {
    return AclData.fromAcl(acl)
        .setMetadata(
            Resource.Metadata.builder()
                .setSelf(
                    urlFactory
                        .newUrlBuilder()
                        .appendPathSegment("v3")
                        .appendPathSegment("clusters")
                        .appendPathSegment(acl.getClusterId())
                        .appendPathSegment("acls")
                        .putQueryParameter("resource_type", acl.getResourceType().name())
                        .putQueryParameter("resource_name", acl.getResourceName())
                        .putQueryParameter("pattern_type", acl.getPatternType().name())
                        .putQueryParameter("principal", acl.getPrincipal())
                        .putQueryParameter("host", acl.getHost())
                        .putQueryParameter("operation", acl.getOperation().name())
                        .putQueryParameter("permission", acl.getPermission().name())
                        .build())
                .build())
        .build();
  }
}
