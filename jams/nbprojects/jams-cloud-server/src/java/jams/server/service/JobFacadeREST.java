/*
 * UserFacadeREST.java
 * Created on 01.03.2014, 21:37:11
 *
 * This file is part of JAMS
 * Copyright (C) FSU Jena
 *
 * JAMS is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JAMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JAMS. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package jams.server.service;

import jams.server.entities.Job;
import jams.server.entities.JobState;
import jams.server.entities.Jobs;
import jams.server.entities.User;
import jams.server.entities.Workspace;
import jams.server.entities.WorkspaceFileAssociation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;


/**
 *
 * @author Sven Kralisch <sven.kralisch at uni-jena.de>
 */
@Stateless
@Path("job")
public class JobFacadeREST extends AbstractFacade<Job> {

    Logger logger = Logger.getLogger(JobFacadeREST.class.getName());

    @PersistenceContext(unitName = "jams-serverPU")
    private EntityManager em;

    static ProcessManager processManager =
            System.getProperty("os.name").contains("Windows") ? new Win64ProcessManager() :
            System.getProperty("os.name").contains("Linux") ? new LinuxProcessManager() :
            new LinuxProcessManager();

    public JobFacadeREST() {
        super(Job.class);
    }

    private List<Workspace> getWorkspaceWithID(int id){
        return em.createQuery(
                "SELECT w FROM Workspace w WHERE w.id = :id")
                .setParameter("id", id)
                .getResultList();
    }

    private List<Job> getJobsForUser(int ownerID){
        return em.createQuery(
                "SELECT w FROM Job w WHERE w.ownerID = :ownerID")
                .setParameter("ownerID", ownerID)
                .getResultList();
    }

    private List<Job> getAllJobs() {
        return em.createQuery(
                "SELECT w FROM Job w ")
                .getResultList();
    }

    private Job updateJob(Job job){
        if (job.getPID()==-1){
            return job;
        }
        try{
            JobState state = processManager.state(job);
            if (!state.isActive()){
                job.setPID(-1);
            }
            Workspace ws = processManager.updateWorkspace(job, em);
            job.setWorkspace(ws);
            em.persist(job);
            return job;
        }catch(IOException ioe){
            logger.log(Level.SEVERE,ioe.getMessage(),ioe);
        }
        return job;
    }

    private List<Job> updateJobs(List<Job> jobs){
        ArrayList<Job> returnList = new ArrayList<>();
        for (Job job : jobs){
            returnList.add(updateJob(job));
        }
        return returnList;
    }

    private Workspace duplicateWorkspace(Workspace ws){
        Workspace ws_clone = new Workspace(ws);
        ws_clone.setReadOnly(true);
        getEntityManager().persist(ws_clone);
        getEntityManager().flush();
        getEntityManager().refresh(ws_clone);
        for (WorkspaceFileAssociation wfa : ws.getFiles()){
            ws_clone.assignFile(wfa.getFile(), wfa.getRole(), wfa.getPath());
        }
        getEntityManager().persist(ws_clone);
        getEntityManager().flush();
        getEntityManager().refresh(ws_clone);
        return ws_clone;
    }

    @GET
    @Path("create")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response create(@QueryParam("workspace") Integer wsID,
            @QueryParam("file") Integer wfaID, @Context HttpServletRequest req) {

        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        //perform cleanup at first
        try{
            processManager.cleanUp(currentUser, new Jobs(findAll()));
        }catch(IOException ioe){
            Logger.getLogger(getClass().getName()).warning(ioe.toString());
        }

        List<Workspace> list = getWorkspaceWithID(wsID);
        if (list == null || list.isEmpty()){
            return Response.status(Status.NOT_FOUND).build();
        }
        Workspace ws = list.get(0);
        if (ws.getUser().getId() != currentUser.getId() || ws.isReadOnly()){
            return Response.status(Status.FORBIDDEN).build();
        }
        ws = duplicateWorkspace(list.get(0));
        WorkspaceFileAssociation modelWFA = null;
        for (WorkspaceFileAssociation wfa : ws.getFiles()){
            if (wfa.getFile().getId().equals(wfaID))
                modelWFA = wfa;
        }
        if (modelWFA == null){
            return Response.status(Status.NOT_FOUND).build();
        }
        Job job = new Job(0, currentUser, ws, modelWFA);
        if (!create(job) || job.getId()==null){
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        try{
            job = processManager.deploy(job);
            em.persist(job);
            return Response.ok(job).build();
        }catch(Throwable ioe){
            Logger logger = Logger.getLogger(JobFacadeREST.class.getName());
            logger.log(Level.SEVERE,ioe.getMessage(),ioe);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("find")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response find(@Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        List<Job> list = getJobsForUser(currentUser.getId());
        //do update
        list = updateJobs(list);
        if (list == null){
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(new Jobs(list)).build();
    }

    @GET
    @Path("reset")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response reset(@Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        List<Job> list = getJobsForUser(currentUser.getId());
        if (list != null){
            for (Job job : list){
                remove(job);
            }
        }
        return Response.ok("Successful").build();
    }

    @GET
    @Path("findActive")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response activeJobs(@Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        List<Job> list = getJobsForUser(currentUser.getId());
        if (list == null || list.isEmpty()){
            return Response.status(Status.NOT_FOUND).build();
        }
        ArrayList<Job> activeList = new ArrayList<Job>();
        //do update
        list = updateJobs(list);

        for (Job job : list){
            if (job.getPID()>-1){
                activeList.add(job);
            }
        }

        return Response.ok(new Jobs(activeList)).build();
    }

    @GET
    @Path("findAll")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response findAll(@Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (currentUser.getAdmin()==0)
            return Response.status(Status.FORBIDDEN).build();

        List<Job> list = getAllJobs();
        list = updateJobs(list);
        if (list == null || list.isEmpty()){
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(new Jobs(list)).build();
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getJob(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);

        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);

        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        updateJob(job);
        return Response.ok(job).build();
    }

    @GET
    @Path("{id}/infolog")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response infolog(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);

        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);

        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            StreamingOutput so = processManager.streamInfoLog(job);
            return Response.ok(so).header("fileName", "info.log").build();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{id}/errorlog")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response errorlog(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);

        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);

        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            StreamingOutput so = processManager.streamErrorLog(job);
            return Response.ok(so).header("fileName", "error.log").build();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{id}/state")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getState(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);

        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);

        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            updateJob(job);
            JobState state = processManager.state(job);
            return Response.ok(state).build();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("load")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response load(@Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        double load = processManager.getLoad();
        return Response.ok(Double.toString(load)).build();
    }

    @GET
    @Path("{id}/kill")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response kill(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);

        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);

        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            JobState state = processManager.kill(job);
            updateJob(job);
            return Response.ok(state).build();
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{id}/delete")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response delete(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);
        
        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        // If user is not owner and not admin, deny
        if (job.getOwner().getId() != currentUser.getId() && currentUser.getAdmin() != 1) {
            return Response.status(Status.FORBIDDEN).build();
        }

        try {
            JobState state = processManager.kill(job);
            if (state != null && state.isActive()) {
                return Response.status(Status.REQUEST_TIMEOUT).build();
            } else {
                Workspace ws = job.getWorkspace();
                super.remove(job);
                Utilities.deleteWorkspace(currentUser, ws);
                em.remove(ws);
            }
            return Response.ok(job).build();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{id}/refresh")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response refresh(@PathParam("id") Integer id, @Context HttpServletRequest req) {
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Job job = find(id);
        if (job == null){
            return Response.status(Status.NOT_FOUND).build();
        }
        if (job.getOwner().getId() != currentUser.getId()){
            return Response.status(Status.FORBIDDEN).build();
        }

        job = updateJob(job);

        Workspace ws = job.getWorkspace();
        return Response.ok(ws).build();
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
