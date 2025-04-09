package io.github.cue.clipboardbridge.client.infrastructure.service;

import io.github.cue.clipboardbridge.client.domain.service.ClipboardService;
import org.springframework.stereotype.Service;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса для работы с системным буфером обмена.
 * Использует стандартные средства Java AWT и альтернативные методы для разных ОС.
 */
@Service
@Slf4j
public class SystemClipboardService implements ClipboardService {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClipboardAvailable() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                log.warn("Графическая подсистема недоступна (headless окружение)");
                // Даже в headless режиме пробуем использовать альтернативные методы
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    // Для Windows сначала сделаем тестовое копирование в буфер обмена
                    boolean testCopy = copyToClipboardAlternative("test clipboard");
                    if (!testCopy) {
                        log.warn("Тестовое копирование в буфер обмена не удалось");
                        return false;
                    }
                    
                    // Теперь проверим, можем ли мы прочитать буфер обмена
                    try {
                        String test = getFromClipboardAlternative();
                        log.info("Проверка доступности буфера обмена через альтернативный метод: {}", 
                                (test != null ? "УСПЕШНО" : "НЕУДАЧА"));
                        return test != null;
                    } catch (Exception e) {
                        log.warn("Проверка буфера обмена через альтернативный метод завершилась ошибкой: {}", e.getMessage());
                return false;
                    }
                }
                return os.contains("linux") || os.contains("unix") || os.contains("mac");
            }
            
            // Проверка доступности системного буфера обмена
            Toolkit.getDefaultToolkit().getSystemClipboard();
            return true;
        } catch (Exception e) {
            log.error("Буфер обмена недоступен: {}", e.getMessage());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean copyToClipboard(String text) {
        if (text == null) {
            log.warn("Попытка копирования null в буфер обмена");
            return false;
        }
        
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return copyToClipboardAlternative(text);
            }
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, selection);
            log.debug("Текст успешно скопирован в буфер обмена");
            return true;
        } catch (Exception e) {
            log.error("Ошибка при копировании в буфер обмена: {}", e.getMessage());
            return copyToClipboardAlternative(text);
        }
    }
    
    /**
     * Альтернативный метод копирования текста в буфер обмена Windows с использованием командной строки
     * 
     * @param text Текст для копирования
     * @return true если копирование успешно, false если произошла ошибка
     */
    private boolean copyToClipboardAlternative(String text) {
        log.debug("Использование альтернативного метода копирования в буфер обмена");
        try {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Пробуем несколько методов для Windows по очереди
                boolean success = copyToClipboardWindows_PowerShell(text);
                if (!success) {
                    success = copyToClipboardWindows_ClipExe(text);
                }
                return success;
            } else if (os.contains("linux") || os.contains("unix")) {
                // Для Linux xclip или xsel
                String[] command = {"xclip", "-selection", "clipboard"};
                Process process = Runtime.getRuntime().exec(command);
                process.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
                return process.waitFor() == 0;
            } else if (os.contains("mac")) {
                // Для macOS pbcopy
                String[] command = {"pbcopy"};
                Process process = Runtime.getRuntime().exec(command);
                process.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
                return process.waitFor() == 0;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Альтернативное копирование не удалось: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Копирование в буфер обмена Windows через PowerShell
     */
    private boolean copyToClipboardWindows_PowerShell(String text) {
        try {
            log.debug("Копирование в буфер обмена Windows через PowerShell");
            
            // Создаем временный файл с текстом
            Path tempFile = Files.createTempFile("clipboard", ".txt");
            Files.writeString(tempFile, text, StandardCharsets.UTF_8);
            
            // Используем PowerShell для копирования текста из файла в буфер обмена
            String[] command = {
                "powershell.exe", 
                "-Command", 
                "Get-Content -Path \"" + tempFile.toString() + "\" -Raw | Set-Clipboard"
            };
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            // Считываем возможные ошибки
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorLine;
                StringBuilder errorOutput = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append(System.lineSeparator());
                }
                
                if (errorOutput.length() > 0) {
                    log.warn("PowerShell вернул ошибки: {}", errorOutput);
                }
            }
            
            // Удаляем временный файл
            Files.delete(tempFile);
            
            log.debug("PowerShell завершился с кодом: {}", exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Ошибка при копировании через PowerShell: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Копирование в буфер обмена Windows через clip.exe
     */
    private boolean copyToClipboardWindows_ClipExe(String text) {
        try {
            log.debug("Копирование в буфер обмена Windows через clip.exe");
            
            // Создаем временный файл с текстом
            Path tempFile = Files.createTempFile("clipboard", ".txt");
            Files.writeString(tempFile, text, StandardCharsets.UTF_8);
            
            // Создаем bat-файл для правильной обработки перенаправления через |
            Path batchFile = Files.createTempFile("clipboard", ".bat");
            Files.writeString(batchFile, 
                    String.format("@echo off\r\ntype \"%s\" | clip", tempFile.toString()));
            
            Process process = Runtime.getRuntime().exec("cmd.exe /c " + batchFile.toString());
            int exitCode = process.waitFor();
            
            // Удаляем временные файлы
            Files.delete(tempFile);
            Files.delete(batchFile);
            
            log.debug("Clip.exe завершился с кодом: {}", exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Ошибка при копировании через clip.exe: {}", e.getMessage());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFromClipboard() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return getFromClipboardAlternative();
            }
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String text = (String) clipboard.getData(DataFlavor.stringFlavor);
                log.debug("Текст успешно получен из буфера обмена");
                return text;
            } else {
                log.warn("В буфере обмена нет текстовых данных");
                return null;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            log.error("Ошибка при получении данных из буфера обмена: {}", e.getMessage());
            return getFromClipboardAlternative();
        }
    }
    
    /**
     * Альтернативный метод чтения текста из буфера обмена Windows с использованием командной строки
     * 
     * @return Текст из буфера обмена или null, если произошла ошибка
     */
    private String getFromClipboardAlternative() {
        log.debug("Использование альтернативного метода чтения из буфера обмена");
        try {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Пробуем сначала PowerShell метод
                String content = getFromClipboardWindows_PowerShell();
                
                // Если не удалось через PowerShell, пробуем другие методы
                if (content == null) {
                    content = getFromClipboardWindows_Alternative();
                }
                
                return content;
            } else if (os.contains("linux") || os.contains("unix")) {
                // Для Linux xclip
                Process process = Runtime.getRuntime().exec(new String[] {"xclip", "-selection", "clipboard", "-o"});
                process.waitFor();
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else if (os.contains("mac")) {
                // Для macOS pbpaste
                Process process = Runtime.getRuntime().exec(new String[] {"pbpaste"});
                process.waitFor();
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Альтернативное чтение не удалось: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Чтение из буфера обмена Windows через PowerShell
     */
    private String getFromClipboardWindows_PowerShell() {
        try {
            log.debug("Чтение буфера обмена Windows с помощью PowerShell и временного файла");
            
            // Создаем временный файл для хранения содержимого буфера обмена
            Path tempFile = Files.createTempFile("clipboard_out", ".txt");
            
            // Используем PowerShell для записи содержимого буфера в файл
            String[] command = {
                "powershell.exe", 
                "-Command", 
                "Get-Clipboard | Out-File -FilePath \"" + tempFile.toString() + "\" -Encoding utf8"
            };
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            // Считываем возможные ошибки
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorLine;
                StringBuilder errorOutput = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append(System.lineSeparator());
                }
                
                if (errorOutput.length() > 0) {
                    log.warn("PowerShell вернул ошибки: {}", errorOutput);
                }
            }
            
            log.debug("PowerShell завершился с кодом: {}, выходной файл: {}", exitCode, tempFile);
            
            if (exitCode == 0) {
                // Проверяем, что файл действительно создан и не пуст
                File file = tempFile.toFile();
                if (file.exists() && file.length() > 0) {
                    String content = Files.readString(tempFile, StandardCharsets.UTF_8);
                    Files.delete(tempFile);
                    
                    if (content.trim().length() > 0) {
                        log.debug("Успешно прочитано {} символов из буфера обмена через PowerShell", content.length());
                        return content;
                    } else {
                        log.warn("Файл буфера обмена оказался пустым (только пробелы)");
                    }
                } else {
                    log.warn("Файл буфера обмена не создан или пуст: {}", tempFile);
                }
            }
            
            // Удаляем временный файл
            try {
                Files.delete(tempFile);
            } catch (Exception e) {
                // Игнорируем, если файл не может быть удален
            }
            
            log.warn("Не удалось прочитать буфер обмена через PowerShell");
            return null;
        } catch (Exception e) {
            log.error("Ошибка при чтении из буфера обмена через PowerShell: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Альтернативный метод чтения из буфера обмена Windows (прямой вывод из PowerShell)
     */
    private String getFromClipboardWindows_Alternative() {
        try {
            log.debug("Прямое чтение буфера обмена Windows с помощью PowerShell");
            
            // Используем PowerShell с прямым выводом
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-Command", "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.Clipboard]::GetText()"
            );
            
            Process process = pb.start();
            
            // Ждем завершения и читаем вывод
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && output.length() > 0) {
                log.debug("Успешно прочитано {} символов из буфера обмена через альтернативный PowerShell метод", 
                        output.length());
                return output.toString();
            }
            
            log.warn("Альтернативный PowerShell метод не смог прочитать буфер обмена");
            return null;
        } catch (Exception e) {
            log.error("Ошибка при чтении из буфера обмена через альтернативный PowerShell метод: {}", e.getMessage());
            return null;
        }
    }
} 