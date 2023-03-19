package projects.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.x.protobuf.MysqlxPrepare.Prepare;

import projects.entity.Category;
import projects.entity.Material;
import projects.entity.Project;
import projects.entity.Step;
import projects.exception.DbException;
import provided.util.DaoBase;

/*
 * DAO layer class that uses JDBC to perform CRUD operations on the DB
 */
public class ProjectDao extends DaoBase {
	private static final String CATEGORY_TABLE = "category";
	private static final String MATERIAL_TABLE = "material";
	private static final String PROJECT_TABLE = "project";
	private static final String PROJECT_CATEGORY_TABLE = "project_category";
	private static final String STEP_TABLE = "step";

	/*
	 * Method to return a specified project by project ID and its materials, steps, and categories
	 */
	public Optional<Project> fetchProjectById(Integer projectId) {
		String sql = "SELECT * FROM " + PROJECT_TABLE + " WHERE project_id = ?";
		
		try(Connection conn = DbConnection.getConnection()) {
			startTransaction(conn);
			/*
			 * Try/catch block to either execute the code to return the project with its
			 * details, or roll back the transaction if an error occurs.
			 */
			try {
				Project project = null;
				try(PreparedStatement stmt = conn.prepareStatement(sql)) {
					setParameter(stmt, 1, projectId, Integer.class);
					try(ResultSet rs = stmt.executeQuery()) {
						if (rs.next()) {
							project = extract(rs, Project.class);
						}
					}
				}
				
				// quality of life null check to prevent unnecessary DB calls.
				if (Objects.nonNull(project)) {
				project.getMaterials().addAll(fetchMaterialsForProject(conn, projectId));
				project.getSteps().addAll(fetchStepsForProject(conn, projectId));
				project.getCategories().addAll(fetchCategoriesForProject(conn, projectId));
				}
				commitTransaction(conn);
				
				return Optional.ofNullable(project);
				
			} catch (Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		} catch(SQLException e) {
			throw new DbException(e);
		}
	}

/*
 * Method to return the materials associated with the project id passed in.
 * Transaction is started by the calling method fetchProjectById()
 */
private List<Material> fetchMaterialsForProject(Connection conn,
		Integer projectId) throws SQLException {
	String sql = "SELECT * FROM " + MATERIAL_TABLE + " WHERE project_id = ?";
	
	try(PreparedStatement stmt = conn.prepareStatement(sql)) {
		setParameter(stmt, 1, projectId, Integer.class);
		
		try(ResultSet rs = stmt.executeQuery()) {
			List<Material> materials = new LinkedList<>();
			while (rs.next()) {
				materials.add(extract(rs, Material.class));
				}
				return materials;
			}
		}
	}

/*
 * Method to return the steps associated with the project id passed in.
 * Transaction is started by the calling method fetchProjectById()
 */
private List<Step> fetchStepsForProject(Connection conn,
	Integer projectId) throws SQLException {
		String sql = "SELECT * FROM " + STEP_TABLE + " WHERE project_id = ?";
		
		try(PreparedStatement stmt = conn.prepareStatement(sql)) {
			setParameter(stmt, 1, projectId, Integer.class);
			
			try(ResultSet rs = stmt.executeQuery()) {
				List<Step> steps = new LinkedList<>();
				while (rs.next()) {
					steps.add(extract(rs, Step.class));
				}
				return steps;
			}
		}
	}

/*
 * Method that returns all categories tied to the project ID passed in.
 * Join since the category table and project table have a many-to-many relationship.
 * Transaction is started by the calling method fetchProjectById()
 */
private List<Category> fetchCategoriesForProject(Connection conn,
		Integer projectId) throws SQLException {
	// @formatter:off
	String sql = ""
			+ "SELECT c.* FROM " + CATEGORY_TABLE + " c "
			+ "JOIN " + PROJECT_CATEGORY_TABLE + " pc USING (category_id) "
			+ "WHERE project_id = ?";
	// @formatter:on
	
	try(PreparedStatement stmt = conn.prepareStatement(sql)) {
		setParameter(stmt, 1, projectId, Integer.class);
		
		try(ResultSet rs = stmt.executeQuery()) {
			List<Category> categories = new LinkedList<>();
			while (rs.next()) {
				categories.add(extract(rs, Category.class));
			}
			return categories;
		}
	}
}

/*
 *  Method returns all projects in the projects table.
 *  Doesn't include materials, steps, or categories.
 */
public List<Project> fetchAllProjects() {
	String sql = "SELECT * FROM " + PROJECT_TABLE + " ORDER BY project_name";
	
	try(Connection conn = DbConnection.getConnection()) {
		startTransaction(conn);
		
		try(PreparedStatement stmt = conn.prepareStatement(sql)) {
			try(ResultSet rs = stmt.executeQuery()) {
				List<Project> projects = new LinkedList<>();
				
				while(rs.next()) {
					projects.add(extract(rs, Project.class));
				}
				
				return projects;
			}
			
		} catch(Exception e) {
			rollbackTransaction(conn);
			throw new DbException(e);
		}
	} catch(SQLException e) {
		throw new DbException(e);
	}
}
	
	/*
	 * Method to insert a row in the project table.
	 */
	public Project insertProject(Project project) {
		// @formatter:off
		String sql = ""
				+ "INSERT INTO " + PROJECT_TABLE + " "
				+ "(project_name, estimated_hours, actual_hours, difficulty, notes) "
				+ "VALUES "
				+ "(?, ?, ?, ?, ?)";
		// @formatter:on
		
		try(Connection conn = DbConnection.getConnection()) {
			startTransaction(conn);
			
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				setParameter(stmt, 1, project.getProjectName(), String.class);
				setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
				setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
				setParameter(stmt, 4, project.getDifficulty(), Integer.class);
				setParameter(stmt, 5, project.getNotes(), String.class);
				
				stmt.executeUpdate();
				
				Integer projectId = getLastInsertId(conn, PROJECT_TABLE);
				commitTransaction(conn);
				
				project.setProjectId(projectId);
				return project;
			} catch (Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public boolean modifyProjectDetails(Project project) {
		// @formatter:off
		String sql = "UPDATE " + PROJECT_TABLE + " SET "
				+ "project_name = ?, "
				+ "estimated_hours = ?, "
				+ "actual_hours = ?, "
				+ "difficulty = ?, "
				+ "notes = ? "
				+ "WHERE project_id = ?";
		// @formatter:on
		
		try(Connection conn = DbConnection.getConnection()) {
			startTransaction(conn);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				setParameter(stmt, 1, project.getProjectName(), String.class);
				setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
				setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
				setParameter(stmt, 4, project.getDifficulty(), Integer.class);
				setParameter(stmt, 5, project.getNotes(), String.class);
				// encountered error about not having a 6th stmt. Still need to pass a project ID to the DB, duh.
				setParameter(stmt, 6, project.getProjectId(), Integer.class);
				
				boolean updated = stmt.executeUpdate() == 1;
				commitTransaction(conn);
				
				return updated;
				
			} catch (Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public boolean deleteProject(Integer projectId) {
		String sql = "DELETE FROM " + PROJECT_TABLE + " WHERE project_id = ?";
		
		try(Connection conn = DbConnection.getConnection()) {
			startTransaction(conn);
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				setParameter(stmt, 1, projectId, Integer.class);
				
				boolean updated = stmt.executeUpdate() == 1;
				commitTransaction(conn);
				
				return updated;
				
			} catch (Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}
}
