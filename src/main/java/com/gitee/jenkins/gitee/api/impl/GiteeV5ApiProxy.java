package com.gitee.jenkins.gitee.api.impl;

import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;


/**
 * @author Robin Müller
 * @author Yashin Luo
 *
 */
@Path("/api/v5")
interface GiteeV5ApiProxy extends GiteeApiProxy {
    String ID = "v5";
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/repos/{ownerPath}/{repoPath}/pulls/{prNumber}/merge")
    void acceptPullRequest(@PathParam("ownerPath") String ownerPath,
                            @PathParam("repoPath") String repoPath,
                            @PathParam("prNumber") Integer prNumber);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/repos/{ownerPath}/{repoPath}/pulls/{prNumber}/comments")
    void createPullRequestNote(@PathParam("ownerPath") String ownerPath,
                                @PathParam("repoPath") String repoPath,
                                @PathParam("prNumber") Integer prNumber,
                                @FormParam("body") String body);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/repos/{ownerPath}/{repoPath}/pulls")
    void createPullRequest(@PathParam("ownerPath") String ownerPath,
                            @PathParam("repoPath") String repoPath,
                            @FormParam("title") String title,
                            @FormParam("base") String base,
                            @FormParam("head") String head,
                            @FormParam("body") String body,
                            @FormParam("prune_source_branch") Boolean pruneSourceBranch,
                            @FormParam("draft") Boolean isDraft,
                            @FormParam("squash") Boolean isSquashMerge);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/repos/{ownerPath}/{repoPath}/pulls")
    List<PullRequest> getPullRequest(@PathParam("ownerPath") String ownerPath,
                                @PathParam("repoPath") String repoPath,
                                @QueryParam("base") String base,
                                @QueryParam("head") String head);

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    void headCurrentUser();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    User getCurrentUser();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/repos/{ownerPath}/{repoPath}/labels")
    List<Label> getLabels(@PathParam("ownerPath") String ownerPath,
                        @PathParam("repoPath") String repoPath);

}
