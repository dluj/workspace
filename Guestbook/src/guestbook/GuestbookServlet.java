package guestbook;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class GuestbookServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		String name = req.getParameter("name");
		resp.getWriter().println("" +
				"<html><head><link type=\"text/css\" rel=\"stylesheet\" href=\"/stylesheets/main.css\" /></head><body>");
		if(user != null){
			resp.setContentType("text/html");
			String form = "<label>Greet something mofo!</label><br /><form action=\"/guestbook\" method=\"post\"><div><textarea name=\"content\" rows=\"3\" cols=\"60\"></textarea></div><div><input type=\"submit\" value=\"Post Greeting\" /></div><input type=\"hidden\" name=\"guestbookName\" value=\"${fn:escapeXml(guestbookName)}\"/></form>";
			String logout_div = "<div id=\"logout\"> <a href=<\""+userService.createLogoutURL(req.getRequestURI())+"\">logout</a> </div>";
			resp.getWriter().println(logout_div);
			resp.getWriter().println(form);

			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Key guestbookKey = KeyFactory.createKey("Guestbook", user.getEmail());
			// Run an ancestor query to ensure we see the most up-to-date
			// view of the Greetings belonging to the selected Guestbook.
			Query query = new Query("Greeting", guestbookKey).addSort("date", Query.SortDirection.DESCENDING);
			List<Entity> greetings = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
			for(Entity greeting : greetings){
				String greet_user = greeting.getProperty("user").toString();
				String greet_message = greeting.getProperty("content").toString();
				String greet = "<div id=\"greet\">USER:"+greet_user+"<br />MESSAGE:"+greet_message+"</div>";
				resp.getWriter().println(greet);
			}
			
			if(greetings.size() == 0){
				resp.getWriter().println("NO GREETINGS FOR YOU");
			}
			resp.getWriter().println("</body></html>");
			
		}else{
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();

		// We have one entity group per Guestbook with all Greetings residing
		// in the same entity group as the Guestbook to which they belong.
		// This lets us run a transactional ancestor query to retrieve all
		// Greetings for a given Guestbook.  However, the write rate to each
		// Guestbook should be limited to ~1/second.

		Key guestbookKey = KeyFactory.createKey("Guestbook", user.getEmail());
		String content = req.getParameter("content");
		Date date = new Date();
		Entity greeting = new Entity("Greeting", guestbookKey);
		greeting.setProperty("user", user);
		greeting.setProperty("date", date);
		greeting.setProperty("content", content);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(greeting);
		resp.sendRedirect("/guestbook");
	}
}
