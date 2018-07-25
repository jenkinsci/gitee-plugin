package com.gitee.jenkins.gitee.api.impl;

import com.gitee.jenkins.gitee.api.model.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


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

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    void headCurrentUser();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    User getCurrentUser();

}
