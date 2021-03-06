package mx.com.develop.senduser;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //<editor-fold desc="VARIABLES DE INSTANCIA">
    TextView tv_nombre, tv_telefono;
    ImageView iv_contacto;
    Button btn_seleccionar_contacto, btn_enviar;

    public static final int PICK_CONTACT_REQUEST = 1;
    private Uri contactUri;
    //</editor-fold>

    //<editor-fold desc="MÉTODO CALLBACK onCreate()">
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarComponentesUI();
        inicializarSetOnclickListener();
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO inicializarComponentesUI()">
    private void inicializarComponentesUI() {
        tv_nombre = (TextView) findViewById(R.id.tv_nombre);
        tv_telefono = (TextView) findViewById(R.id.tv_telefono);
        iv_contacto = (ImageView) findViewById(R.id.iv_contacto);
        btn_seleccionar_contacto = (Button) findViewById(R.id.btn_seleccionar_contacto);
        btn_enviar = (Button) findViewById(R.id.btn_enviar);
    }
    //</editor-fold>

    //<editor-fold desc="CONTROLADOR DE ENVENTOS setOnclickListener">
    private void inicializarSetOnclickListener() {
        btn_seleccionar_contacto.setOnClickListener(this);
        btn_enviar.setOnClickListener(this);
    }
    //</editor-fold>

    //<editor-fold desc="CONTROLADOR DE EVENTOS onClick(View v">
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_seleccionar_contacto:
                actionPickContactos();
                break;

            case R.id.btn_enviar:
                sendMessage();
                break;
        }
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO actionPickContactos()">
    public void actionPickContactos() {

        /* Crear un intent para seleccionar un contacto del dispositivo */
        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);

       /*Iniciar la actividad esperando respuesta a través del canal PICK_CONTACT_REQUES */
        startActivityForResult(i, PICK_CONTACT_REQUEST);
    }
    //</editor-fold>

    //<editor-fold desc=" MÉTODO onActivityResult()">
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
            /* Capturar el valor de la Uri */
                contactUri = intent.getData();
            /* Procesar la Uri*/
                extraerContacto(contactUri);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO extraerContacto(Uri uri">
    private void extraerContacto(Uri uri) {

        tv_nombre.setText(getName(uri));
        tv_telefono.setText(getPhone(uri));
        iv_contacto.setImageBitmap(getPic(uri));
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO getName(Uri uri)">
    private String getName(Uri uri) {

        /* Valor a retornar */
        String name = null;

        /* Obtener una instancia del Content Resolver*/
        ContentResolver contentResolver = getContentResolver();

        /*Cursor para recorrer los datos de la consulta*/
        Cursor c = contentResolver.query(
                uri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);

        /* Consultando el primer y único resultado elegido */
        if (c.moveToFirst()) {
            name = c.getString(0);
        }

        /*Cerramos el cursor*/
        c.close();

        return name;
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO getPhone(Uri uri)">
    private String getPhone(Uri uri) {

        //<editor-fold desc="Variables temporales para el id y el teléfono ">
        String id = null;
        String phone = null;
        //</editor-fold>

        //<editor-fold desc="PRIMERA CONSULTA  Obtener el _ID del contacto ">
        Cursor contactCursor = getContentResolver().query( uri, new String[]{ContactsContract.Contacts._ID},null, null, null);

        if (contactCursor.moveToFirst()) {
            id = contactCursor.getString(0);
        }
        contactCursor.close();
        //</editor-fold>

        //<editor-fold desc="SEGUNDA CONSULTA  Sentencia WHERE para especificar que solo deseamos números de telefonía móvil">
        String selectionArgs =
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                        ContactsContract.CommonDataKinds.Phone.TYPE + "= " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        //</editor-fold>

        //<editor-fold desc="Obtener el número telefónico">
        Cursor phoneCursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                selectionArgs,
                new String[]{id},
                null);
        //</editor-fold>

        if (phoneCursor.moveToFirst()) {
            phone = phoneCursor.getString(0);
        }
        phoneCursor.close();

        return phone;
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO getPic(Uri uri)">
    private Bitmap getPic(Uri uri) {

        /*Foto del contacto y su id  */
        Bitmap photo = null;
        String id = null;

        //<editor-fold desc="CONSULTA">
        Cursor contactCursor = getContentResolver().query(
                uri, new String[]{ContactsContract.Contacts._ID}, null, null, null);
        //</editor-fold>

        if (contactCursor.moveToFirst()) {
            id = contactCursor.getString(0);
        }
        contactCursor.close();

        /* Usar el método de clase openContactPhotoInputStream()*/
        try {
            InputStream input =
                    ContactsContract.Contacts.openContactPhotoInputStream(
                            getContentResolver(),
                            ContentUris.withAppendedId(
                                    ContactsContract.Contacts.CONTENT_URI,
                                    Long.parseLong(id))
                    );
            if (input != null) {
                /*Dar formato tipo Bitmap a los bytes del BLOB correspondiente a  la foto */
                photo = BitmapFactory.decodeStream(input);
                input.close();
            }
        } catch (IOException iox) { /* Manejo de errores */ }

        return photo;
    }
    //</editor-fold>

    //<editor-fold desc="MÉTODO sendMessage()">
    public void sendMessage() {

        /* Creando el gestor de mensajes*/
        SmsManager smsManager = SmsManager.getDefault();

        /*Enviando el mensaje*/
        if (contactUri != null) {
            smsManager.sendTextMessage(String.valueOf((contactUri)), null, "¡Estas aprendiendo a Desarrollar en Android!", null, null);

            Toast.makeText(this, "Mensaje Enviado", Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(this, "Selecciona un contacto primero", Toast.LENGTH_LONG).show();
    }
    //</editor-fold>


}
