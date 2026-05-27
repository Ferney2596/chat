package com.poli.persistencia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Proyecto Grupal: Persistencia de Datos y Transaccionalidad
 * Entrega 1 - Semana 3: Cliente de Chat Bidireccional
 */
public class ChatClient {
    // Configuración de red local especificada por el grupo
    private static final String IP_SERVIDOR = "127.0.0.1"; 
    private static final int PUERTO = 59420; 

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);
        
        System.out.println("=== Ventana de Conectividad del Cliente ==="); 
        System.out.print("Escriba su nombre de usuario (ej. poli01): "); 
        String usuario = teclado.nextLine();

        // Establecer el canal de comunicación a través del socket TCP/IP
        try (Socket socket = new Socket(IP_SERVIDOR, PUERTO); 
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream()); 
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

            // Enviar credencial de identificación al servidor de inmediato
            salida.writeUTF(usuario);
            
            // Recibir respuesta de validación por parte del servidor
            String estadoConexion = entrada.readUTF();
            if (estadoConexion.equals("RECHAZADO")) {
                System.out.println("Error: El nombre de usuario ya está en uso o no es válido.");
                return;
            }

            System.out.println("\nConexion establecida exitosamente con " + IP_SERVIDOR + ":" + PUERTO);
            System.out.println("Para salir de la sesion del chat, escriba la palabra: chao\n"); 

            // Levantar hilo secundario para escuchar de manera asíncrona los mensajes entrantes del servidor
            Thread escucharServidor = new Thread(() -> {
                try {
                    while (true) {
                        String msg = entrada.readUTF();
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    // Se interrumpe la lectura cuando el socket principal se cierra formalmente
                    System.out.println("Conexion con el servidor finalizada correctamente.");
                }
            });
            escucharServidor.start();

            // Hilo principal se encarga exclusivamente de capturar lo que el estudiante escribe en consola
            String lineaMensaje;
            do {
                lineaMensaje = teclado.nextLine();
                // Enviar la línea transaccional al flujo de salida del Socket
                salida.writeUTF(lineaMensaje);
            } while (!lineaMensaje.equalsIgnoreCase("chao")); 
            
            System.out.println("Saliendo del sistema de comunicaciones...");

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor de chat. Verifique que esté encendido.");
        } finally {
            teclado.close();
        }
    }
}