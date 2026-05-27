package com.poli.persistencia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proyecto Grupal: Persistencia de Datos y Transaccionalidad
 * Entrega 1 - Semana 3: Servidor de Chat Bidireccional
 */
public class ChatServer {
    // Puerto establecido en la arquitectura del diseño de red del grupo
    private static final int PUERTO = 59420;
    
    // Diccionario concurrente para llevar el registro en tiempo real de los clientes conectados
    private static final ConcurrentHashMap<String, Socket> clientesConectados = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== Log del Servidor ===");
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado y contestando OK en el puerto " + PUERTO); 

            while (true) {
                // El servidor se queda en escucha pasiva esperando conexiones de clientes
                Socket socketCliente = serverSocket.accept(); 
                
                // Delegar la atención del cliente a un hilo dedicado para no bloquear el servidor
                new Thread(new ClientHandler(socketCliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        }
    }

    // Clase interna encargada de procesar la comunicación individual de cada cliente
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String nombreUsuario;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                DataInputStream entrada = new DataInputStream(socket.getInputStream());
                DataOutputStream salida = new DataOutputStream(socket.getOutputStream())
            ) {
                // 1. Fase de Autenticación / Registro de Credenciales
                this.nombreUsuario = entrada.readUTF(); 
                
                // Validar si el nombre ya existe para evitar colisiones
                if (clientesConectados.containsKey(nombreUsuario) || nombreUsuario.trim().isEmpty()) {
                    salida.writeUTF("RECHAZADO");
                    socket.close();
                    return;
                }

                // Confirmar conexión y registrar en el Log global
                salida.writeUTF("ACEPTADO");
                clientesConectados.put(nombreUsuario, socket);
                System.out.println("Usuario \"" + nombreUsuario + "\" conectado"); 
                
                // Notificar al cliente la lista actual de participantes con los que puede interactuar
                enviarListaUsuarios();

                // 2. Fase de Comunicación Transaccional de Mensajes
                String mensajeCliente;
                while (true) {
                    mensajeCliente = entrada.readUTF(); 
                    
                    // Condición de cierre dictada por la rúbrica del proyecto
                    if (mensajeCliente.equalsIgnoreCase("chao")) { 
                        System.out.println("El usuario \"" + nombreUsuario + "\" abandonó"); 
                        break;
                    }
                    
                    // Loguear la conversación recibida en la consola del servidor
                    System.out.println("[" + nombreUsuario + "]: " + mensajeCliente);
                }

            } catch (IOException e) {
                // Manejo de desconexiones abruptas del socket
                if (nombreUsuario != null && clientesConectados.containsKey(nombreUsuario)) {
                    System.out.println("Conexion perdida inesperadamente con: " + nombreUsuario);
                }
            } finally {
                // Limpieza de recursos y actualización del estado del servidor
                if (nombreUsuario != null) {
                    clientesConectados.remove(nombreUsuario);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar socket de " + nombreUsuario);
                }
            }
        }

        // Método auxiliar para reportar la trazabilidad de usuarios activos
        private void enviarListaUsuarios() {
            StringBuilder lista = new StringBuilder("Usuarios en línea actualmente: ");
            for (String user : clientesConectados.keySet()) {
                lista.append("[").append(user).append("] ");
            }
            System.out.println("Log actualizado de usuarios en línea: " + lista);
        }
    }
}