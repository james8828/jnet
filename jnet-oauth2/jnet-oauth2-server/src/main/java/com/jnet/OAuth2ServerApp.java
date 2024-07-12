package com.jnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class OAuth2ServerApp
{
    public static void main( String[] args )
    {
        System.out.println( "Oauth2ServerApp start!" );
        SpringApplication.run(OAuth2ServerApp.class, args);
    }
}
