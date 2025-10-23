package com.projects.document_organizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocumentOrganizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentOrganizerApplication.class, args);
	}

}
