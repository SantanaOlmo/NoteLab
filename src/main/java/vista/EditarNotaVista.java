package vista;

import bbdd.GestorBBDD;
import modelo.Nota;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Clase EditarNotaVista que representa una ventana de diálogo
 * para crear y guardar una nueva nota con título, contenido y hashtags.
 *
 * ➤ Usa estilo visual coherente con el resto de la app (modo oscuro).
 * ➤ Guarda los datos en la base de datos incluyendo relación con hashtags.
 * ➤ Se lanza desde el botón "+" en la interfaz principal. */

public class EditarNotaVista {

    //-----------------------------------------------------------------------------
    // ATRIBUTOS
    //-----------------------------------------------------------------------------

    private final JDialog dialogo;
    private final JTextField campoTitulo;
    private final JTextPane campoHashtags;
    private final JTextPane campoContenido;
    private final JPanel panelBotones;
    private final JButton btnGuardar;

    //-----------------------------------------------------------------------------
    // CONSTRUCTOR
    //-----------------------------------------------------------------------------

    public EditarNotaVista(JFrame padre) {
        // Se crea un cuadro de diálogo modal que se cierra antes de volver al menú
        dialogo = new JDialog(padre, "Nueva Nota", true);
        dialogo.setSize(400, 300);
        dialogo.setLocationRelativeTo(null);
        dialogo.setLayout(new BorderLayout());

        campoTitulo = new JTextField();
        dialogo.add(campoTitulo, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel();
        panelCentro.setLayout(new BoxLayout(panelCentro, BoxLayout.Y_AXIS));

        campoHashtags = new JTextPane();
        JScrollPane scrollHashtags = new JScrollPane(campoHashtags);
        scrollHashtags.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panelCentro.add(scrollHashtags);

        campoContenido = new JTextPane();
        panelCentro.add(new JScrollPane(campoContenido));

        dialogo.add(panelCentro, BorderLayout.CENTER);

        panelBotones = new JPanel(new FlowLayout());
        btnGuardar = new JButton("Guardar");
        panelBotones.add(btnGuardar);
        dialogo.add(panelBotones, BorderLayout.SOUTH);

        //-----------------------------------------------------------------------------
        // EVENTO: Guardar nota
        //-----------------------------------------------------------------------------

        // Cuando se pulsa "Guardar", se intenta guardar la nota y hashtags
        btnGuardar.addActionListener(e -> guardarNota());

        //-----------------------------------------------------------------------------
        // ESTILO VISUAL OSCURO
        //-----------------------------------------------------------------------------

        Color fondo = new Color(43, 43, 43);
        Color campos = new Color(30, 30, 30);
        Color textoClaro = new Color(220, 220, 220);
        Color bordeClaro = new Color(100, 100, 100);

        dialogo.getContentPane().setBackground(fondo);
        panelCentro.setBackground(fondo);
        panelBotones.setBackground(fondo);

        campoTitulo.setBackground(campos);
        campoTitulo.setForeground(textoClaro);
        TitledBorder bordeTitulo = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(bordeClaro), "Título");
        bordeTitulo.setTitleColor(textoClaro);
        campoTitulo.setBorder(bordeTitulo);

        campoHashtags.setBackground(campos);
        campoHashtags.setForeground(textoClaro);
        TitledBorder bordeHashtags = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(bordeClaro), "Hashtags");
        bordeHashtags.setTitleColor(textoClaro);
        campoHashtags.setBorder(bordeHashtags);

        campoContenido.setBackground(campos);
        campoContenido.setForeground(textoClaro);
        TitledBorder bordeContenido = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(bordeClaro), "Contenido");
        bordeContenido.setTitleColor(textoClaro);
        campoContenido.setBorder(bordeContenido);

        btnGuardar.setBackground(new Color(60, 63, 65));
        btnGuardar.setForeground(textoClaro);
        btnGuardar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setContentAreaFilled(true);
    }

    //-----------------------------------------------------------------------------
    // MÉTODO: Mostrar diálogo
    //-----------------------------------------------------------------------------

    public void mostrar() {
        dialogo.setVisible(true);
    }

    //-----------------------------------------------------------------------------
    // MÉTODO: Guardar nota y hashtags
    //-----------------------------------------------------------------------------

    private void guardarNota() {
        String titulo = campoTitulo.getText();
        String contenido = campoContenido.getText();
        String textoHashtags = campoHashtags.getText();

        // Comprobación mínima para que no se guarden notas vacías
        if (!titulo.isEmpty() && !contenido.isEmpty()) {
            try {
                Connection connection = GestorBBDD.getConnection();

                // Guardar la nota en la base de datos
                String sqlNota = "INSERT INTO notas(titulo, contenido) VALUES (?, ?)";
                PreparedStatement psNota = connection.prepareStatement(sqlNota, PreparedStatement.RETURN_GENERATED_KEYS);
                psNota.setString(1, titulo);
                psNota.setString(2, contenido);
                psNota.executeUpdate();

                // Obtener el ID generado automáticamente
                ResultSet rsNota = psNota.getGeneratedKeys();
                int notaId = -1;
                if (rsNota.next()) {
                    notaId = rsNota.getInt(1);
                }
                rsNota.close();
                psNota.close();

                // Procesar hashtags si se han escrito
                if (textoHashtags != null && !textoHashtags.trim().isEmpty()) {
                    String[] tagsSeparados = textoHashtags.split(",");
                    for (String tag : tagsSeparados) {
                        String texto = tag.trim().toLowerCase();
                        if (texto.startsWith("#")) {
                            texto = texto.substring(1);
                        }

                        // Insertar hashtag si no existe
                        String insertarHashtag = "INSERT IGNORE INTO hashtags(nombre) VALUES (?)";
                        PreparedStatement ps1 = connection.prepareStatement(insertarHashtag);
                        ps1.setString(1, texto);
                        ps1.executeUpdate();
                        ps1.close();

                        // Obtener ID del hashtag insertado o existente
                        String buscarHashtag = "SELECT id FROM hashtags WHERE nombre = ?";
                        PreparedStatement ps2 = connection.prepareStatement(buscarHashtag);
                        ps2.setString(1, texto);
                        ResultSet rs = ps2.executeQuery();

                        int hashtagId = -1;
                        if (rs.next()) {
                            hashtagId = rs.getInt("id");
                        }
                        rs.close();
                        ps2.close();

                        // Crear relación entre la nota y el hashtag
                        if (hashtagId != -1) {
                            String insertarRelacion = "INSERT IGNORE INTO nota_hashtag(nota_id, hashtag_id) VALUES (?, ?)";
                            PreparedStatement ps3 = connection.prepareStatement(insertarRelacion);
                            ps3.setInt(1, notaId);
                            ps3.setInt(2, hashtagId);
                            ps3.executeUpdate();
                            ps3.close();
                        }
                    }
                }

                // Mostrar mensaje y cerrar la ventana
                JOptionPane.showMessageDialog(dialogo, "Nota guardada correctamente.");
                dialogo.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialogo, "Error al guardar la nota.");
            }
        } else {
            JOptionPane.showMessageDialog(dialogo, "Por favor completa todos los campos.");
        }
    }

    //-----------------------------------------------------------------------------
    // MÉTODOS GETTERS
    //-----------------------------------------------------------------------------

    public JTextField getCampoTitulo() {
        return campoTitulo;
    }

    public JTextPane getCampoHashtags() {
        return campoHashtags;
    }

    public JTextPane getCampoContenido() {
        return campoContenido;
    }

    public JPanel getPanelBotones() {
        return panelBotones;
    }

    public JDialog getDialogo() {
        return dialogo;
    }

    public JButton getBtnGuardar() {
        return btnGuardar;
    }
}
