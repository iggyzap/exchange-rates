package controllers;

import play.mvc.*;



public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void index90() {
        render("Application/all-90.html");
    }

}