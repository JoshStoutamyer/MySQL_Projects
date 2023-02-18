package projects;


import projects.dao.DbConnection;

public class ProjectApp {

	public static void main(String[] args) {

		DbConnection.getConnection();
		
	}

}
