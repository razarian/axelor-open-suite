/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.apps.tool.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileTool {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileTool.class);

	private FileTool(){
		
	}
	
	/**
	 * Méthode permettant de lire le contenu d'un fichier
	 * 
	 * @param fileName
	 * 			Le nom du fichier
	 * @return
	 * 			Une liste contenant l'ensemble des lignes
	 * @throws IOException 
	 * @throws AxelorException 
	 */
	public static List<String> reader(String fileName) throws IOException {
		
		List<String> content = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));
		String ligne = "";
		
		while ((ligne = br.readLine()) != null)  {
			content.add(ligne);
		}
		
		br.close();
			
	   return content;
	}
	
	
	/**
	 * Méthode permettant d'écrire une ligne dans un fichier
	 * @param destinationFolder
	 * 				Le chemin du fichier
	 * @param fileName
	 * 				Le nom du fichier
	 * @param line
	 * 				La ligne à écrire
	 * @throws IOException
	 */
	public static void writer(String destinationFolder, String fileName, String line) throws IOException {
		System.setProperty("line.separator", "\r\n");
		FileWriter writer = null;
		
		try  {
			
			 File file = create(destinationFolder, fileName);
		     writer = new FileWriter(file);
		     writer.write(line);
		     
		}  
		catch(IOException ex)  {
			
			LOG.error(ex.getMessage());
		}  
		finally  {
			
			if(writer != null)  { writer.close(); }

		}
	}
	
	/**
	 * Méthode permettant d'écrire plusieurs lignes dans un fichier
	 * @param destinationFolder
	 * 				Le chemin du fichier
	 * @param fileName
	 * 				Le nom du fichier
	 * @param line
	 * 				La liste de ligne à écrire
	 * @throws IOException
	 */
	public static void writer(String destinationFolder, String fileName, List<String> multiLine) throws IOException {
		System.setProperty("line.separator", "\r\n");
		BufferedWriter output = null;
		try  {
			
			 File file = create(destinationFolder, fileName);
		     output = new BufferedWriter(new FileWriter(file));
		     int i = 0;
		     
		     for (String line : multiLine)   {
		    	 
		    	 output.write(line);
		    	 output.newLine();
		    	 i++;
		    	 if (i % 50 == 0){ output.flush(); }
		    	 
		     }
		     
		}  
		catch(IOException ex)  {
			
			LOG.error(ex.getMessage());
			
		}  
		finally  {
			
			if(output != null) { output.close(); }
		}
	}
	
	/**
	 * Création d'un fichier avec son chemin si il n'existe pas
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static File create(String fileName) throws IOException{
		
		String[] filePath = fileName.split(System.getProperty("file.separator"));
		String name = filePath[filePath.length - 1];
		return create(fileName.replace(name, ""), name);
	}
	
	/**
	 * Création d'un fichier avec son chemin si il n'existe pas
	 * 
	 * @param destinationFolder
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static File create(String destinationFolder, String fileName) throws IOException {
		
		File folder = new File(destinationFolder);
		if (!folder.exists()) { folder.mkdirs(); }
		
		return new File(folder, fileName);
	}
	
	
	/**
	 * Méthode permettant de copier le fichier vers une destination
	 * 
	 * @param fileSrc
	 * 			Le chemin du fichier source
	 * @param fileDest
	 * 			Le chemin du fichier destination
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws AxelorException 
	 */
	public static void copy(String fileSrc, String fileDest) throws IOException  {

		IOUtils.copy(new FileInputStream(fileSrc), new FileOutputStream(fileDest));
		
	}
	
	/**
	 * Copy all files and directories from a Folder to a destination Folder.
	 * Must be called like: listAllFilesInFolder(srcFolderPath, "", srcFolderPath,
	 * destFolderPath)
	 * 
	 * @param currentFolder Used for the recursive called.
	 * @param relatedPath Used for the recursive called.
	 * @param sourceFolder Source directory.
	 * @param destinationFolder Destination directory.
	 * @param logger A logger.
	 * @throws IOException 
	 */
	public static void copyFolderToFolder(String sourceFolder, String destinationFolder) throws IOException {
		
		// Current Directory.
		File current = new File(sourceFolder);
		File destFile = new File(destinationFolder);
		
		if (!destFile.exists()) { destFile.mkdir(); }
		
		if (current.isDirectory()) {
			
			// List all files and folder in the current directory.
			File[] list = current.listFiles();
			if (list != null) {
				// Read the files list.
				for (int i = 0; i < list.length; i++) {
					// Create current source File
					File tf = new File(sourceFolder + "/" + list[i].getName());
					// Create current destination File
					File pf = new File(destinationFolder + "/" + list[i].getName());
					if (tf.isDirectory() && !pf.exists()) {
						// If the file is a directory and does not exit in the
						// destination Folder.
						// Create the directory.
						pf.mkdir();
						copyFolderToFolder(tf.getAbsolutePath(), pf.getAbsolutePath());
					} else if (tf.isDirectory() && pf.exists()) {
						// If the file is a directory and exits in the
						// destination Folder.
						copyFolderToFolder(tf.getAbsolutePath(), pf.getAbsolutePath());
					} else if (tf.isFile()) {
						// If it is a file.
						copy(sourceFolder + "/" + list[i].getName(), destinationFolder + "/" + list[i].getName());
					} else {
						// Other cases.
						LOG.error("Error : Copy folder");
					}
				}
			}
		}
	}
	
}
