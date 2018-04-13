/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backup;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author alexisbrabo
 */
public class Backup {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {

        Properties config = new Properties();
        try (InputStream inFile = new FileInputStream((new File("dados.properties")).getAbsoluteFile())) {
            config.load(inFile);
        }

        String DB_HOST = config.getProperty("DB_HOST");
        String DB_USER = config.getProperty("DB_USER");
        String DB_USER_PASSWORD = config.getProperty("DB_USER_PASSWORD");
        String DB_PORT = config.getProperty("DB_PORT");
        // String DB_NAME = "/backup/dbnames.txt";
        // Se você tiver varias databases listadas em um arquivo, descomente a linha acima e substitua o caminho do diretório.
        String DB_NAME = config.getProperty("DB_NAME");
        String DIRETORIO_BACKUP = config.getProperty("DIRETORIO_BACKUP");
        String DIRETORIO_XML = config.getProperty("DIRETORIO_XML");
        String DIRETORIO_CERTIFICADO = config.getProperty("DIRETORIO_CERTIFICADO");
        // Pegar hora para botar como nome pra pasta
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy-HHmm");
        String DATETIME = dateFormat.format(new Date());
        File DIRETORIO_BACKUP_HOJE = new File(DIRETORIO_BACKUP + DATETIME);
        Integer diasParaApagar = Integer.parseInt(config.getProperty("diasParaApagar"));
        boolean multi;

        // Checando se a pasta já existe
        System.out.println("Criando pasta de backup");
        if (!DIRETORIO_BACKUP_HOJE.exists()) {
            DIRETORIO_BACKUP_HOJE.mkdirs();
        }
        File diretorioGeral = new File(DIRETORIO_BACKUP);

        File[] files = diretorioGeral.listFiles((File f) -> f.isDirectory());

        System.out.println("Pastas: " + files.length);

        for (File f : files) {
            if (f.isDirectory()) {
                long diff = new Date().getTime() - f.lastModified();
                if (diff > diasParaApagar * 24 * 60 * 60 * 1000) {
                    for (File fi : f.listFiles()) {
                        fi.delete();
                    }
                    f.delete();
                }
            }
        }

        System.out.println("checando arquivo de databases.");
        File arquivoDatabases = new File(DB_NAME);
        if (arquivoDatabases.exists()) {
            multi = true;
            System.out.println("O arquivo de dbs foi encontrado...");
            System.out.println("Começando o backup de todos os bancos listados... " + DB_NAME);
        } else {
            System.out.println("O arquivo de dbs não foi encontrado...");
            System.out.println("Começando o backup " + DB_NAME);
            multi = false;
        }

        if (!multi) {
            String db = DB_NAME;

            String dumpcmd = "mysqldump -u " + DB_USER + " -p" + DB_USER_PASSWORD + " -P " + DB_PORT + " -B " + db + " > " + DIRETORIO_BACKUP_HOJE + "/" + db + ".sql";

            Process exec;

            if (System.getProperty("os.name").toLowerCase().equals("linux")) {
                exec = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", dumpcmd});
            } else {
                exec = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", dumpcmd});
            }

            //esperar o comando acabar e checar se o valor exit e 0
            if (exec.waitFor() == 0) {
                //normally terminated, a way to read the output
                InputStream inputStream = exec.getInputStream();
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);

                String str = new String(buffer);
                System.out.println(str);
            } else {
                // problema
                // ler erro do comando
                InputStream errorStream = exec.getErrorStream();
                byte[] buffer = new byte[errorStream.available()];
                errorStream.read(buffer);

                String str = new String(buffer);
                System.out.println(str);
                return;
            }

            zipFolder(Paths.get(DIRETORIO_BACKUP_HOJE + "/" + db + ".sql"), Paths.get(DIRETORIO_BACKUP_HOJE + "/automacao.zip"));
            boolean deleteFile = new File(DIRETORIO_BACKUP_HOJE + "/" + db + ".sql").delete();

            db = "autorizanfe";

            dumpcmd = "mysqldump -u " + DB_USER + " -p" + DB_USER_PASSWORD + " -P " + DB_PORT + " -B " + db + " > " + DIRETORIO_BACKUP_HOJE + "/" + db + ".sql";

            if (System.getProperty("os.name").toLowerCase().equals("linux")) {
                exec = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", dumpcmd});
            } else {
                exec = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", dumpcmd});
            }

            //esperar o comando acabar e checar se o valor exit e 0
            if (exec.waitFor() == 0) {
                //normally terminated, a way to read the output
                InputStream inputStream = exec.getInputStream();
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);

                String str = new String(buffer);
                System.out.println(str);
            } else {
                // problema
                // ler erro do comando
                InputStream errorStream = exec.getErrorStream();
                byte[] buffer = new byte[errorStream.available()];
                errorStream.read(buffer);

                String str = new String(buffer);
                System.out.println(str);
                return;
            }
            zipFolder(Paths.get(DIRETORIO_BACKUP_HOJE + "/" + db + ".sql"), Paths.get(DIRETORIO_BACKUP_HOJE + "/autorizanfe.zip"));
            deleteFile = new File(DIRETORIO_BACKUP_HOJE + "/" + db + ".sql").delete();

        }

        if (!DIRETORIO_CERTIFICADO.equals("")) {
            zipFolder(Paths.get(DIRETORIO_CERTIFICADO), Paths.get(DIRETORIO_BACKUP_HOJE + "/repositorioA1.zip"));
        }
        if (!DIRETORIO_XML.equals("")) {
            zipFolder(Paths.get(DIRETORIO_XML), Paths.get(DIRETORIO_BACKUP_HOJE + "/repositorioxml.zip"));
        }
        System.out.println("Backup completo");
        System.out.println("Seu backup foi criado em '" + DIRETORIO_BACKUP_HOJE + "' diretorio");
    }

    private static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception {

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));

                    Files.copy(file, zos);

                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;

                }

            });
        }

    }

}
