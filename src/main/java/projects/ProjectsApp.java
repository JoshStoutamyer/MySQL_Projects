package projects;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import projects.entity.Project;
import projects.exception.DbException;
import projects.service.ProjectService;

/*
 * Menu driven app that collects user input via scanner 
 * and performs CRUD operations on the DB tables.
 */
public class ProjectsApp {
	private Scanner scanner = new Scanner(System.in);
	private ProjectService projectService = new ProjectService();
	private Project curProject;
	
	// @formatter:off
	private List<String> operations = List.of(
		"1) Add a project",
		"2) List projects",
		"3) Select a project",
		"4) Update project details",
		"5) Delete a project"
	);
	// @formatter:on
	
	/*
	 * Main method for the app. The entry point for the user.
	 */
	public static void main(String[] args) {
		new ProjectsApp().processUserSelections();
	}
	
	/*
	 * Correlates to the operations List above. Collects user selection.
	 * Performs the action tied to the number selected by the user.
	 * Will continue to repeat until user exits application.
	 */
	private void processUserSelections() {
		boolean done = false;
		
		while (!done) {
			try {
				int selection = getUserSelection();
				
				switch (selection) {
				case -1:
					done = exitMenu();
					break;
				case 1:
					creatProject();
					break;
				case 2:
					listProjects();
					break;
				case 3:
					selectProject();
					break;
				case 4:
					updateProjectDetails();
					break;
				case 5:
					deleteProject();
					break;
					
					
					default:
						System.out.println("\n" + selection + " is not a valid selection. Try again.");
						break;
				}
				// handles errors so that the application can keep running 'gracefully'
			} catch (Exception e) {
				System.out.println("\nError: " + e + " Try again.");
			}
		}
	}
	
	/*
	 * Deletes a selected project, by calling the list of available projects and getting user input
	 * to select the project_id to delete.
	 * Invalid IDs are handled at the business layer.
	 */
	private void deleteProject() {
		listProjects();
		Integer projectId = getIntInput("Enter the project ID to delete");
		
		projectService.deleteProject(projectId);
		System.out.println("Project ID=" + projectId + " was deleted.");
		// Reverts current project to null to create a clean slate for the next selection.
		if(Objects.nonNull(curProject) && curProject.getProjectId().equals(projectId)) {
			curProject = null;
		}
	}
	
	/*
	 * Updates several of the columns associated with a user selected project ID. 
	 * Does not include materials, steps, or categories.
	 */
	private void updateProjectDetails() {
		// Check if a project is selected. Will return user to the operations menu to make a valid selection.
		if (Objects.isNull(curProject)) {
			System.out.println("\nPlease select a project.");
		return;
		}
		
		// Calling fields from the Projects class.
		String projectName = getStringInput("Enter the project name [" + curProject.getProjectName() + "]");
		BigDecimal estimatedHours = getDecimalInput("Enter the project estimate hours [" 
				+ curProject.getEstimatedHours() + "]");
		BigDecimal actualHours = getDecimalInput("Enter the project actual hours [" 
				+ curProject.getActualHours() + "]");
		Integer difficulty = getIntInput("Enter the project difficulty [" + curProject.getDifficulty() + "]");
		String notes = getStringInput("Enter the project notes [" + curProject.getNotes() + "]");
		
		// New Project object to hold user input.
		Project project = new Project();
		
		// Project ID will remain the same. Does not need to be user input.
		project.setProjectId(curProject.getProjectId());
		// Setter to use the user input to update the fields
		project.setProjectName(Objects.isNull(projectName) ? curProject.getProjectName() : projectName);
		project.setEstimatedHours(Objects.isNull(estimatedHours) ? curProject.getEstimatedHours() : estimatedHours);
		project.setActualHours(Objects.isNull(actualHours) ? curProject.getActualHours() : actualHours);
		project.setDifficulty(Objects.isNull(difficulty) ? curProject.getDifficulty() : difficulty);
		project.setNotes(Objects.isNull(notes) ? curProject.getNotes() : notes);
		
		// Sending the selected project ID & updated information to the Service layer.
		projectService.modifyProjectDetails(project);
		curProject = projectService.fetchProjectById(curProject.getProjectId());

		
	}
	
	/*
	 * Method for user to select the current project.
	 * Required step for adding steps, materials, and categories and for updating.
	 */
	private void selectProject() {
		listProjects();
		Integer projectId = getIntInput("Enter a project ID to select a project");
		// Sets current project to null to avoid errors from invalid IDs thrown by fetchProjectId().
		curProject = null;
		
		curProject = projectService.fetchProjectById(projectId);
		// A contingency if an invalid ID is selected.
		if (Objects.isNull(projectId)) {
			System.out.println("Invalid project ID selected.");
		}
	}

	/*
	 * Method to call for the list of available projects
	 */
	private void listProjects() {
		List<Project> projects = projectService.fetchAllProjects();
		
		System.out.println("\nProjects:");
		
		// Lambda to print the project names to console
		projects.forEach(project -> System.out.println("  " + project.getProjectId() 
			+ ": " + project.getProjectName()));
	}

	/*
	 * Method to create a project in the project table.
	 * Gets user input for each of the Project class fields,
	 * then sets those values under a new project ID.
	 */
	private void creatProject() {
		String projectName = getStringInput("Enter the project name");
		BigDecimal estimateHours =  getDecimalInput("Enter the estimate hours");
		BigDecimal actualHours = getDecimalInput("Enter the actual hours");
		Integer difficulty = getIntInput("Enter the project difficulty (1-5)");
		String notes = getStringInput("Enter the project notes");
		
		Project project = new Project();
		
		project.setProjectName(projectName);
		project.setEstimatedHours(estimateHours);
		project.setActualHours(actualHours);
		project.setDifficulty(difficulty);
		project.setNotes(notes);
		
		// call to the Service layer
		Project dbProject = projectService.addProject(project);
		// Print to console on successful creation of new project
		System.out.println("You have successfully created project: " + dbProject);
		
	}
	
	/*
	 * First stage of gathering user input for time related fields
	 * getStringInput() will then return it to a string.
	 */
	private BigDecimal getDecimalInput(String prompt) {
		String input = getStringInput(prompt);
		
		// returns null if no input is detected.
		if (Objects.isNull(input)) {
			return null;
		}
		try {
			// returns the created BigDecimal object with a limit of two decimal points
			return new BigDecimal(input).setScale(2);
		} catch (NumberFormatException e) {
			throw new DbException(input + " is not a valid decimal number.");
		}
	}
	
	/*
	 * Method to cleanly exit the menu.
	 */
	private boolean exitMenu() {
		System.out.println("\nExiting the menu. Buh bye~");
		return true;
	}

	/*
	 * Method to print menu items and gather user input to convert to int.
	 */
	private int getUserSelection() {
		printOperations();
		Integer input = getIntInput("Enter a menu selection");
		// returns -1 if null or returns input if not.
		return Objects.isNull(input) ? -1 : input;
	}

	/*
	 * Gathers the user input and converts to int. 
	 */
	private Integer getIntInput(String prompt) {
		String input = getStringInput(prompt);
		
		// null returned if the user enters nothing.
		if (Objects.isNull(input)) {
			return null;
		}
		try {
			return Integer.valueOf(input);
			// exception to handle invalid entries.
		} catch (NumberFormatException e) {
			throw new DbException(input + " is not a valid number. Try again.");
		}
	}
	
	/*
	 * Gathers user input from prompt. Returns null if nothing entered. Trims the returned input.
	 */
	private String getStringInput(String prompt) {
		System.out.print(prompt + ": ");
		String input = scanner.nextLine();
		return input.isBlank() ? null : input.trim();
	}
	
	/*
	 * Prints menu options from the operations List.
	 */
	private void printOperations() {
		System.out.println("\nThese are the available selections. Press the Enter key to quit:");
		// Lambda to print each operation in the list.
		operations.forEach(line -> System.out.println("   " + line));
		// Message to let the user know where they are in the selection process.
		if(Objects.isNull(curProject)) {
			System.out.println("\nYou are not working with a project.");
		} else {
			System.out.println("\nYou are working with project: " + curProject);
		}
	}

}
