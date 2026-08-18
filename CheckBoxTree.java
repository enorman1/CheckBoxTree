
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.InputMap;

import javax.swing.UIManager;
//import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Insets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.text.Collator;




/**
 * Type d'élément affiché dans l'explorateur.
 */
enum FileNodeType {
	CURRENT_DIRECTORY,
	PARENT_DIRECTORY,
	DIRECTORY,
	FILE
}


/**
 * Objet représentant un élément de l'arbre.
 */
class FileNode {
	private final File file;
	private final FileNodeType type;
	private final long size;
	private final long lastModified;
	
	public FileNode(File file, FileNodeType type) {
		this.file = file;
		this.type = type;
		/*
		 * Récupération des métadonnées des fichiers.
		 */
		if (file != null && file.exists()) {
			this.size = file.length();
			this.lastModified = file.lastModified();
		} else {
			this.size = 0L;
			this.lastModified = 0L;
		}
	}
	public File getFile() {
		return file;
	}
	public FileNodeType getType() {
		return type;
	}
	public long getSize() {
		return size;
	}
	public long getLastModified() {
		return lastModified;
	}
	public boolean isDirectory() {
		return type == FileNodeType.DIRECTORY
				|| type == FileNodeType.PARENT_DIRECTORY;
	}
	public boolean isFile() {
		return type == FileNodeType.FILE;
	}
	public boolean isParentDirectory() {
		return type == FileNodeType.PARENT_DIRECTORY;
	}
	@Override
	public String toString() {
		if (isParentDirectory()) {
			return ".." + FileSystems.getDefault().getSeparator();
		}
		return file.getName();
		/*
		switch (type) {
			case CURRENT_DIRECTORY:
				return file.getAbsolutePath();
			case PARENT_DIRECTORY:
				return "../";
			case DIRECTORY:
			case FILE:
			default:
				return file.getName();
		}
		*/
	}
}


/**
 * JTree personnalisé permettant de sélectionner les fichiers.
 */
class JCheckBoxTree extends JTree {
	private final CheckBoxTreeCellRenderer renderer;
	public JCheckBoxTree(DefaultTreeModel model) {
		super(model);
		renderer = new CheckBoxTreeCellRenderer();
		setCellRenderer(renderer);
		setRootVisible(true);
		setShowsRootHandles(true);
		setRowHeight(24);
	}
	/**
	 * Retourne le renderer utilisé par l'arbre.
	 */
	public CheckBoxTreeCellRenderer getCheckBoxRenderer() {
		return renderer;
	}
}


/**
 * Renderer Swing de l'arbre.
 *
 * Affiche :
 *   [Checkbox] [Icône] [Nom] [Taille] [Dernière modification]
 *
 * Le renderer ne stocke PAS l'état de sélection.
 * Il se contente d'afficher l'état fourni par le modèle.
 */
class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
	private final JCheckBox checkBox;
	private final JLabel iconLabel;
	private final JLabel textLabel;
	private final JLabel sizeLabel;
	private final JLabel dateLabel;
	private final FileSystemView fileSystemView;
	private static final Color JAVA_BACKGROUND = new Color(255, 210, 210);
	private static final Color OTHER_BACKGROUND = new Color(240, 240, 255);
	private static final DateTimeFormatter DATE_FORMATTER = 
		DateTimeFormatter.ofPattern("    dd/MM/yyyy HH:mm:ss");
	
	public CheckBoxTreeCellRenderer() {
		//setLayout(new BorderLayout(5, 0));
		setLayout(new FileTreeRowLayout());
		setOpaque(false);
		fileSystemView = FileSystemView.getFileSystemView();
		/*
		 * Checkbox.
		 */
		checkBox = new JCheckBox();
		checkBox.setOpaque(false);
		checkBox.setMargin(new Insets(0, 0, 0, 0)); // Évite que la checkbox possède une marge
		/*
		 * Icône du fichier/dossier.
		 */
		iconLabel = new JLabel();
		/*
		 * Nom du fichier/dossier.
		 */
		textLabel = new JLabel();
		/*
		 * Taille du fichier
		 */
		sizeLabel = new JLabel();
		sizeLabel.setHorizontalAlignment(JLabel.RIGHT); // Alignement à droite pour la taille.
		/*
		 * Date de modification du fichier
		 */
		dateLabel = new JLabel();
		dateLabel.setHorizontalAlignment(JLabel.LEFT); // La date est alignée à gauche.
		
		//add(checkBox, BorderLayout.WEST);
		//add(iconLabel, BorderLayout.CENTER);
		//add(textLabel, BorderLayout.EAST);
		add(checkBox);
		add(iconLabel);
		add(textLabel);
		add(sizeLabel);
		add(dateLabel);
	}
	@Override
	public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean selected,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object userObject = node.getUserObject();
		/*
		 * Nettoyage du renderer
		 */
		checkBox.setVisible(false);
		checkBox.setSelected(false);
		iconLabel.setIcon(null);
		textLabel.setText("");
		textLabel.setBackground(null);
		textLabel.setForeground(Color.BLACK);
		sizeLabel.setText("");
		dateLabel.setText("");
		/*
		 * Objet inattendu
		 */
		if (!(userObject instanceof FileNode)) {
			textLabel.setText(String.valueOf(userObject));
			return this;
		}
		FileNode fileNode = (FileNode) userObject;
		File file = fileNode.getFile();
		
		if (isRootNode(tree, node)) {
			/*
			 * ============================
			 * En-tête du répertoire courant
			 * ============================
			 */
			checkBox.setVisible(true);
			if (tree instanceof FileExplorerTree) {
				FileExplorerTree fileTree = (FileExplorerTree) tree;
				boolean retVal = fileTree.areAllFilesSelected();
				//System.out.println(retVal);
				checkBox.setSelected(fileTree.areAllFilesSelected());
			}
			/*
			 * Pas d'icône dans la colonne
			 * checkbox/icône pour l'en-tête.
			 */
			//iconLabel.setIcon(null);
			iconLabel.setIcon(fileSystemView.getSystemIcon(file));
			/*
			 * Nom du répertoire courant.
			 */
			textLabel.setText(fileNode.toString());
			textLabel.setOpaque(false);
			textLabel.setForeground(Color.BLACK);
			textLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
			/*
			 * En-tête de la colonne Taille.
			 */
			sizeLabel.setText("Size");
			sizeLabel.setHorizontalAlignment(JLabel.RIGHT); // or LEFT
			sizeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
			/*
			 * En-tête de la colonne Date.
			 */
			dateLabel.setText("Date modified");
			dateLabel.setHorizontalAlignment(JLabel.RIGHT); // or RIGHT or CENTER
			dateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
			return this;
		}
		else {
			textLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
			dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		}
		/*
		 * ============================
		 * Icône native
		 * ============================
		 */
		if (file != null) {
			iconLabel.setIcon(fileSystemView.getSystemIcon(file));
		}
		/*
		 * ============================
		 * Nom
		 * ============================
		 */
		textLabel.setText(fileNode.toString());
		/*
		 * ============================
		 * Checkbox
		 * ============================
		 */
		if (fileNode.isFile()) {
			checkBox.setVisible(true);
			if (tree instanceof FileExplorerTree) {
				FileExplorerTree fileTree = (FileExplorerTree) tree;
				checkBox.setSelected(fileTree.isFileSelected(file));
			}
			// Mise en évidence des fichiers Java.
			if (file.getName().toLowerCase().endsWith(".java")) {
				//textLabel.setOpaque(true);
				textLabel.setBackground(JAVA_BACKGROUND);
				textLabel.setForeground(Color.RED);
			}
			else {
				//textLabel.setOpaque(false);
				//textLabel.setOpaque(true);
				textLabel.setBackground(OTHER_BACKGROUND);
				textLabel.setForeground(Color.BLUE);
			}
		}
		else {
			// Dossiers
			textLabel.setOpaque(false);
			textLabel.setForeground(Color.BLACK);
		}
		/*
		 * ============================
		 * Taille & Date de modification
		 * ============================
		 */
		if (fileNode.isFile()) {
			sizeLabel.setText(formatFileSize(fileNode.getSize()));
			dateLabel.setText(formatLastModified(fileNode.getLastModified()));
		}
		else {
			sizeLabel.setText(" "); // Pour les dossiers.
			dateLabel.setText(" ");
		}
		return this;
	}
	
	private boolean isRootNode(JTree tree, DefaultMutableTreeNode node) {
		Object root = tree.getModel().getRoot();
		return root == node;
	}
	
	/**
	 * Convertit une taille exprimée en octets
	 * en une chaîne lisible.
	 */
	private String formatFileSize(long size) {
		if (size < 1024) {
			return size + " o";
		}
		if (size < 1024L * 1024L) {
			return String.format("%.1f Ko", size / 1024.0);
		}
		if (size < 1024L * 1024L * 1024L) {
			return String.format("%.1f Mo", size / (1024.0 * 1024.0));
		}
		return String.format("%.1f Go", size / (1024.0 * 1024.0 * 1024.0));
	}
	
	/**
	 * Convertit le timestamp de dernière
	 * modification en date/heure locale.
	 */
	private String formatLastModified(long timestamp) {
		Instant instant = Instant.ofEpochMilli(timestamp);
		LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		return DATE_FORMATTER.format(dateTime);
	}
}

	/**
	* Layout horizontal spécialisé pour les lignes du JTree.
	* Colonnes :
	*   [Checkbox] [Icône] [Nom] [Taille] [Dernière modification]
	* Les largeurs sont fixes, ce qui permet d'obtenir un véritable
	* alignement vertical entre les différentes lignes du JTree.
	*/
	class FileTreeRowLayout implements LayoutManager {
		/*
		 * Largeurs des colonnes.
		 */
		public static final int CHECKBOX_WIDTH = 30;
		public static final int ICON_WIDTH     = 32;
		public static final int NAME_WIDTH     = 300;
		public static final int SIZE_WIDTH     = 100;
		public static final int DATE_WIDTH     = 150;
		/*
		 * Hauteur d'une ligne.
		 */
		private static final int ROW_HEIGHT = 24;
		@Override
		public void addLayoutComponent(String name, Component comp) {
			// Rien à faire.
		}
		@Override
		public void removeLayoutComponent(Component comp) {
			// Rien à faire.
		}
		@Override
		public Dimension preferredLayoutSize(Container parent) {
			int width = CHECKBOX_WIDTH + ICON_WIDTH + NAME_WIDTH + SIZE_WIDTH + DATE_WIDTH;
			return new Dimension(width, ROW_HEIGHT);
		}
		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}
		@Override
		public void layoutContainer(Container parent) {
			int x = 0;
			int height = parent.getHeight();
			Component[] components = parent.getComponents();
			/*
			 * Ordre attendu :
			 *   0 = checkbox
			 *   1 = icône
			 *   2 = nom
			 *   3 = taille
			 *   4 = date
			 */
			if (components.length > 0) {
				components[0].setBounds(x, 0, CHECKBOX_WIDTH, height);
				x += CHECKBOX_WIDTH;
			}
			if (components.length > 1) {
				components[1].setBounds(x, 0, ICON_WIDTH, height);
				x += ICON_WIDTH;
			}
			if (components.length > 2) {
				components[2].setBounds(x, 0, NAME_WIDTH, height);
				x += NAME_WIDTH;
			}
			if (components.length > 3) {
				components[3].setBounds(x, 0, SIZE_WIDTH, height);
				x += SIZE_WIDTH;
			}
			if (components.length > 4) {
				components[4].setBounds(x, 0, DATE_WIDTH, height);
			}
		}
	}


/**
 * JTree spécialisé pour l'explorateur de fichiers.
 */
class FileExplorerTree extends JTree {
	private final Set<File> selectedFiles;
	public FileExplorerTree(DefaultTreeModel model, Set<File> selectedFiles) {
		super(model);
		this.selectedFiles = selectedFiles;
		setCellRenderer(new CheckBoxTreeCellRenderer());
		setRootVisible(true);
		setShowsRootHandles(true);
		setRowHeight(24);
		/*
		 * Gestion des clics.
		 */
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				}
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				Object userObject = node.getUserObject();
				if (!(userObject instanceof FileNode)) {
					return;
				}
				FileNode fileNode = (FileNode) userObject;
				/*
				 * Double-clic :
				 * uniquement pour naviguer dans les dossiers.
				 */
				if (e.getClickCount() == 2
						&& e.getButton() == MouseEvent.BUTTON1) {
					if (fileNode.isDirectory()) {
						firePropertyChange(
								"directoryDoubleClicked",
								null,
								fileNode.getFile()
						);
					}
					return;
				}
				/*
				 * Simple clic :
				 * uniquement sur les fichiers.
				 */
				if (e.getClickCount() == 1
						&& e.getButton() == MouseEvent.BUTTON1) {
					if (fileNode.isFile()) {
						toggleFile(fileNode.getFile());
						repaint();
						firePropertyChange("selectionChanged", null, fileNode.getFile());
					}
					else {
						//if (path.getPathCount() == 1) { // simple clic sur la racine
						if (node == (DefaultMutableTreeNode) getModel().getRoot()) {
							boolean select = !areAllFilesSelected();
							toggleAllFiles(select);
						}
					}
				}
			}
		});
		setupKeyboardActions(); // gestion du clavier
	}
	
	private void setupKeyboardActions() {
		InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggleFile");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleFile");
		actionMap.put("toggleFile", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreePath path = getSelectionPath();
				if (path == null) {
					return;
				}
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				Object userObject = node.getUserObject();
				if (!(userObject instanceof FileNode)) {
					return;
				}
				FileNode fileNode = (FileNode) userObject;
				/*
				 * On ne permet de cocher/décocher que les fichiers, pas les dossiers.
				 */
				if (fileNode.isFile()) {
					toggleFile(fileNode.getFile());
					repaint();
					firePropertyChange("selectionChanged", null, fileNode.getFile());
				}
				else {
					//if (path.getPathCount() == 1) { // simple clic sur la racine
					if (node == (DefaultMutableTreeNode) getModel().getRoot()) {
						boolean select = !areAllFilesSelected();
						toggleAllFiles(select);
					}
				}
			}
		});
	}
	
	public boolean areAllFilesSelected() {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		return areAllFilesSelected(root);
	}
	
	private boolean areAllFilesSelected(DefaultMutableTreeNode node) {
		boolean FilesIsSelected = false;
		for (int i = 0; i < node.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			Object userObject = child.getUserObject();
			if (userObject instanceof FileNode) {
				FileNode fileNode = (FileNode) userObject;
				if (fileNode.isFile()) {
					if (!selectedFiles.contains(fileNode.getFile())) {
						return false;
					}
					else {
						FilesIsSelected = true;
					}
				}
			}
			//if (!areAllFilesSelected(child)) {
			//	return false;
			//}
		}
		return FilesIsSelected;
	}
	
	private void toggleAllFiles(boolean select) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		toggleFilesRecursively(root, select);
		repaint();
		firePropertyChange("selectionChanged", null, null);
	}
	
	private void toggleFilesRecursively(DefaultMutableTreeNode node, boolean select) {
		for (int i = 0; i < node.getChildCount(); i++) {
			DefaultMutableTreeNode child =
					(DefaultMutableTreeNode) node.getChildAt(i);
			Object userObject = child.getUserObject();
			if (userObject instanceof FileNode) {
				FileNode fileNode = (FileNode) userObject;
				if (fileNode.isFile()) {
					if (select) {
						selectedFiles.add(fileNode.getFile());
					} else {
						selectedFiles.remove(fileNode.getFile());
					}
				}
			}
			// On continue à parcourir les sous-dossiers.
			toggleFilesRecursively(child, select);
		}
	}
	
	private void toggleFile(File file) {
		if (selectedFiles.contains(file)) {
			selectedFiles.remove(file);
		} else {
			selectedFiles.add(file);
		}
	}
	public boolean isFileSelected(File file) {
		return selectedFiles.contains(file);
	}
}


/**
 * Application principale.
 */
public class CheckBoxTree extends JFrame {
	private final JTextArea textArea;
	private final JTextField textPath;
	private final JLabel statusBar;
	private final Set<File> selectedFiles;
	private File currentDirectory;
	private FileExplorerTree tree;
	private DefaultTreeModel treeModel;
	private static String fileSeparator = FileSystems.getDefault().getSeparator();
	
	public CheckBoxTree() {
		/*
		 * ============================
		 * Initialisation
		 * ============================
		 */
		selectedFiles = new LinkedHashSet<File>();
		//currentDirectory = new File(".").getAbsoluteFile();
		currentDirectory = new File(System.getProperty("user.dir")).getAbsoluteFile();
		/*
		 * ============================
		 * Fenêtre
		 * ============================
		 */
		setTitle("My file explorer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		/*
		 * ============================
		 * Menu
		 * ============================
		 */
		createMenuBar();
		/*
		 * ============================
		 * Zone supérieure
		 * ============================
		 */
		JPanel panelZoneCentrale = new JPanel(new BorderLayout());
		panelZoneCentrale.setPreferredSize(new Dimension(800, 150));
		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		JScrollPane textScrollPane = new JScrollPane(textArea);
		panelZoneCentrale.add(textScrollPane, BorderLayout.CENTER);
		JButton buttonAction = new JButton("Action");
		buttonAction.addActionListener(e -> executerAction());
		panelZoneCentrale.add(buttonAction, BorderLayout.EAST);
		add(panelZoneCentrale, BorderLayout.NORTH);
		/*
		 * ============================
		 * Arbre
		 * ============================
		 */
		tree = createTree();
		JScrollPane treeScrollPane = new JScrollPane(tree);
		// Bandeau supérieur avec une bordure en haut et bas
		JPanel panelCenter = new JPanel(new BorderLayout());
		JPanel panelPath = new JPanel(new BorderLayout());
		panelPath.setPreferredSize(new Dimension(600, 40));
		panelPath.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); //top, left, bottom, right
		textPath = new JTextField();
		textPath.setEditable(false);
		// Change text font size
		textPath.setFont(new Font("SansSerif",Font.PLAIN,14)); //(new Font("Serif",Font.BOLD,12));
		// Change text font color
		textPath.setBackground(Color.WHITE);
		textPath.setForeground(Color.BLACK);
		panelPath.add(textPath, BorderLayout.CENTER);
		JButton buttonOpen = new JButton("Open");
		buttonOpen.addActionListener(e -> executerOpen());
		JPanel panelButton = new JPanel(new BorderLayout());
		// Ajoute un espace entre textPath et buttonOpen
		panelButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		panelButton.add(buttonOpen, BorderLayout.CENTER);
		panelPath.add(panelButton,BorderLayout.EAST);
		panelCenter.add(panelPath, BorderLayout.NORTH);
		panelCenter.add(treeScrollPane, BorderLayout.CENTER);
		// Ajoute ces panels à la frame principale
		add(panelCenter, BorderLayout.CENTER);
		/*
		 * ============================
		 * Barre de statut
		 * ============================
		 */
		statusBar = new JLabel();
		statusBar.setHorizontalAlignment(SwingConstants.LEFT);
		statusBar.setBorder(BorderFactory.createEtchedBorder());
		add(statusBar, BorderLayout.SOUTH);
		/*
		 * Chargement initial.
		 */
		loadDirectory(currentDirectory);
		/*
		 * ============================
		 * Fenêtre
		 * ============================
		 */
		setSize(800, 800);
		//setPreferredSize(new Dimension(800, 800));
		setLocationRelativeTo(null);
	}


	/**
	 * Création de l'arbre.
	 */
	private FileExplorerTree createTree() {
		DefaultMutableTreeNode root =
				new DefaultMutableTreeNode(
						new FileNode(
								currentDirectory,
								FileNodeType.DIRECTORY
						)
				);
		treeModel =
				new DefaultTreeModel(root);
		FileExplorerTree fileTree =
				new FileExplorerTree(
						treeModel,
						selectedFiles
				);
		fileTree.addPropertyChangeListener(
				"directoryDoubleClicked",
				evt -> {
					File directory =
							(File) evt.getNewValue();
					loadDirectory(directory);
				}
		);
		fileTree.addPropertyChangeListener(
				"selectionChanged",
				evt -> updateStatusBar()
		);
		return fileTree;
	}

	private static String stripLeadingZeros(String value) {
		int i = 0;
		while (i < value.length() - 1 && value.charAt(i) == '0') {
			i++;
		}
		return value.substring(i);
	}

	private static int naturalCompare(String s1, String s2, Collator collator) {
		int i = 0;
		int j = 0;
		while (i < s1.length() && j < s2.length()) {
			char c1 = s1.charAt(i);
			char c2 = s2.charAt(j);
			// Les deux caractères commencent un nombre
			if (Character.isDigit(c1) && Character.isDigit(c2)) {
				int start1 = i;
				int start2 = j;
				// Cherche la fin du nombre
				while (i < s1.length()
						&& Character.isDigit(s1.charAt(i))) {
					i++;
				}
				while (j < s2.length()
						&& Character.isDigit(s2.charAt(j))) {
					j++;
				}
				String num1 = s1.substring(start1, i);
				String num2 = s2.substring(start2, j);
				// Supprime les zéros à gauche
				//String n1 = num1.replaceFirst("^0+(?!$)", "");
				//String n2 = num2.replaceFirst("^0+(?!$)", "");
				String n1 = stripLeadingZeros(num1);
				String n2 = stripLeadingZeros(num2);
				
				// Compare d'abord la longueur :
				// 2 chiffres < 10 chiffres
				if (n1.length() != n2.length()) {
					return n1.length() < n2.length() ? -1 : 1;
				}
				// Même longueur : comparaison lexicographique
				int result = n1.compareTo(n2);
				if (result != 0) {
					return result;
				}
				// Même valeur numérique.
				// On peut départager "001" et "1".
				if (num1.length() != num2.length()) {
					return num1.length() < num2.length() ? -1 : 1;
				}
				continue;
			}
			// Comparaison de la partie non numérique
			int start1 = i;
			int start2 = j;
			while (i < s1.length()
					&& !Character.isDigit(s1.charAt(i))) {
				i++;
			}
			while (j < s2.length()
					&& !Character.isDigit(s2.charAt(j))) {
				j++;
			}
			String part1 = s1.substring(start1, i);
			String part2 = s2.substring(start2, j);
			int result = collator.compare(part1, part2);
			if (result != 0) {
				return result;
			}
		}
		// Si tout ce qui précède est identique,
		// le plus court vient en premier.
		if (i < s1.length()) {
			return 1;
		}
		if (j < s2.length()) {
			return -1;
		}
		return 0;
	}


	/**
	 * Charge le contenu d'un répertoire.
	 */
	private void loadDirectory(File directory) {
		if (directory == null) {
			return;
		}
		if (!directory.isDirectory()) {
			return;
		}
		/*
		 * La navigation réinitialise la sélection.
		 */
		selectedFiles.clear();
		currentDirectory = directory.getAbsoluteFile();
		/*
		 * Nouveau modèle.
		 */
		DefaultMutableTreeNode root =
				new DefaultMutableTreeNode(
						new FileNode(
								currentDirectory,
								FileNodeType.DIRECTORY
						)
				);
		/*
		 * ============================
		 * Répertoire parent
		 * ============================
		 */
		File parent =
				currentDirectory.getParentFile();
		if (parent != null) {
			root.add(
					new DefaultMutableTreeNode(
							new FileNode(
									parent,
									FileNodeType.PARENT_DIRECTORY
							)
					)
			);
		}
		/*
		 * ============================
		 * Contenu du répertoire
		 * ============================
		 */
		Collator collator = Collator.getInstance(Locale.FRENCH);
		collator.setStrength(Collator.PRIMARY);
		File[] files = currentDirectory.listFiles();
		if (files != null) {
			/*
			 * Sort by :
			 * 1. directories
			 * 2. files
			 * 3. filenames with "natural sort"
			 */
			
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					// 1. Directories before files
					boolean dir1 = f1.isDirectory();
					boolean dir2 = f2.isDirectory();
					if (dir1 && !dir2) {
						return -1;
					}
					if (!dir1 && dir2) {
						return 1;
					}
					// 2. "natural sort" method
					return naturalCompare(f1.getName(), f2.getName(),collator);
					//return f1.getName().compareToIgnoreCase(f2.getName()); // old method
				}
			});
			
			//Arrays.sort(files, WindowsFileComparator.INSTANCE);
			
			for (File file : files) {
				FileNodeType type;
				if (file.isDirectory()) {
					type =
							FileNodeType.DIRECTORY;
				} else {
					type =
							FileNodeType.FILE;
				}
				root.add(
						new DefaultMutableTreeNode(
								new FileNode(
										file,
										type
								)
						)
				);
			}
		}
		/*
		 * Remplacement du modèle.
		 */
		treeModel = new DefaultTreeModel(root);
		tree.setModel(treeModel);
		/*
		 * Mise à jour du titre.
		 */
		//setTitle("My file explorer - " + currentDirectory.getAbsolutePath());
		setTitle("My file explorer - [" + currentDirectory.getName() + "]");
		String str1 = currentDirectory.getAbsolutePath();
		if (!str1.isEmpty()) {
			if (!(fileSeparator.equals(str1.substring(str1.length() - 1))))
				str1 = str1 + fileSeparator;
		}
		else str1 = "." + fileSeparator;
		textPath.setText(str1);
		
		updateStatusBar();
	}


	/**
	 * Mise à jour de la barre de statut.
	 */
	private void updateStatusBar() {
		int count =
				selectedFiles.size();
		if (count == 0) {
			statusBar.setText(
					"No file selected"
			);
		} else if (count == 1) {
			statusBar.setText(
					"1 file selected"
			);
		} else {
			statusBar.setText(
					count
					+ " files selected"
			);
		}
	}


	/**
	 * Action déclenchée par le bouton.
	 */
	private void executerAction() {
		if (selectedFiles.isEmpty()) {
			JOptionPane.showMessageDialog(
					this,
					"No file selected.",
					"Action",
					JOptionPane.INFORMATION_MESSAGE
			);
			return;
		}
		/*
		 * Copie de la sélection.
		 *
		 * Cela évite que le traitement utilise
		 * directement la collection interne.
		 */
		List<File> filesToProcess =
				new ArrayList<File>(
						selectedFiles
				);
		/*
		 * Exemple :
		 * affichage des fichiers sélectionnés.
		 */
		StringBuilder message =
				new StringBuilder();
		message.append(
				"Files selected :\n\n"
		);
		for (File file : filesToProcess) {
			message.append(
					file.getAbsolutePath()
			);
			message.append("\n");
		}
		textArea.setText(
				message.toString()
		);
		/*
		 * C'est ici que devra être placé
		 * le véritable traitement.
		 */
	}


	/**
	 * Choix du répertoire déclenchée par le bouton "Ouvrir"
	 */
	private void executerOpen() {
		//Create a file chooser
		JFileChooser fileChooser = new JFileChooser(){
			@Override
			protected JDialog createDialog( Component parent ) throws HeadlessException {
				JDialog dialog = super.createDialog( parent );
				//dialog.setIconImage(icon);
				return dialog;
			}
		};
		//fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setPreferredSize(new Dimension(600,600));
		//fileChooser.setDialogTitle(buttonOpen.getText());
		fileChooser.setDialogTitle("Choose the working directory");
		//if (fileSeparator.equals(txtPath2.getText()))
		if (!currentDirectory.exists())
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		else
			fileChooser.setCurrentDirectory(currentDirectory);
		int returnVal = fileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			//txtPath2.setText(fileChooser.getPath());
			File file = fileChooser.getSelectedFile();
			if (file.exists())
			{
				// Refresh TreeView
				loadDirectory(file);
			}
		} 
	}


	/**
	 * Création de la barre de menus.
	 */
	private void createMenuBar() {
		JMenuBar menuBar =
				new JMenuBar();
		JMenu menuFichier =
				new JMenu("File");
		JMenuItem itemQuitter =
				new JMenuItem("Quit");
		itemQuitter.addActionListener(
				e -> System.exit(0)
		);
		menuFichier.add(itemQuitter);
		JMenu menuAPropos =
				new JMenu("About");
		JMenuItem itemAPropos =
				new JMenuItem("About");
		itemAPropos.addActionListener(
				e -> afficherBoiteDialogueAPropos()
		);
		menuAPropos.add(itemAPropos);
		menuBar.add(menuFichier);
		menuBar.add(menuAPropos);
		setJMenuBar(menuBar);
	}


	/**
	 * Boîte "À propos".
	 */
	private void afficherBoiteDialogueAPropos() {
		//ImageIcon icon = new ImageIcon("CheckBoxTree.png");
		ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("CheckBoxTree.png"));
		if (icon.getIconWidth() != 128 || icon.getIconHeight() != 128) {
			icon = new ImageIcon(icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH));
		}
		String message = "<html>"
				+ "<div style='text-align: center;'>"
				+ "<h2>My Application</h2>"
				+ "<p><b>Author :</b> Votre Nom</p>"
				+ "<p><b>Creation date :</b> 16/08/2026</p>"
				+ "<p><b>Description :</b> "
				+ "A Java Swing file explorer.</p>"
				+ "</div></html>";
		JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE, icon);
	}


	/**
	 * Point d'entrée.
	 */
	public static void main(String[] args) {
		try {
			boolean UIstyleFound = false;
			UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
			for (UIManager.LookAndFeelInfo look : looks) {
				//System.out.println(look.getClassName());
				if (look.getClassName().toLowerCase().endsWith(".nimbuslookandfeel")) {
					UIstyleFound = true;
				}
			}
			if (UIstyleFound) {
				//UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
				//UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
				//UIManager.setLookAndFeel(new com.sun.java.swing.plaf.motif.MotifLookAndFeel());
				//UIManager.setLookAndFeel(new com.sun.java.swing.plaf.gtk.GTKLookAndFeel());
				//UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
				//UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
				//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
				UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			else {
				System.out.println(UIManager.getSystemLookAndFeelClassName());
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		}
		catch (Exception ex) {
			//ex.printStackTrace();
		}
		finally {
			SwingUtilities.invokeLater(() -> {
						CheckBoxTree application = new CheckBoxTree();
						//application.pack();
						application.setVisible(true);
					}
			);
		}
	}
}
