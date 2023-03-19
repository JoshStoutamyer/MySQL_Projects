package projects.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import projects.dao.ProjectDao;
import projects.entity.Project;
import projects.exception.DbException;

/*
 * Class to implement service layer. Mostly a pass through layer, for this project,
 *  but is used to help separate and keep code clean. 
 */
public class ProjectService {
	private ProjectDao projectDao = new ProjectDao();
	
	
	 // Call the DAO to add a project row
	public Project addProject(Project project) {
		return projectDao.insertProject(project);
	}

	
	// Calls the DAO to return a list of projects (sans details)
	public List<Project> fetchAllProjects() {
		return projectDao.fetchAllProjects();
	}

	/*
	 *  Calls to DAO to return project details of the project ID passed through. 
	 *  throws exception if invalid ID is passed through.
	 */
	public Project fetchProjectById(Integer projectId) {
		return projectDao.fetchProjectById(projectId)
				.orElseThrow( () -> new NoSuchElementException(
						"Project with project ID=" + projectId + " does not exist."));
	}

	/*
	 * Calls the DAO to modify a projects details.
	 * throws exception if the ID passed through is invalid.
	 */
	public void modifyProjectDetails(Project project) {
		if(!projectDao.modifyProjectDetails(project)) {
			throw new DbException("Project with ID=" + project.getProjectId() + " does not exist.");
		}
	}

	/*
	 * Calls to the DAO to delete a project by project_id.
	 * Throws exception if invalid ID is passed through.
	 */
	public void deleteProject(Integer projectId) {
		if (!projectDao.deleteProject(projectId)) {
			throw new DbException("Project with ID=" + projectId + " does not exist.");
		}
		
		
	}
}
