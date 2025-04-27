package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.FilteringTree
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import com.intellij.util.SystemProperties
import com.intellij.util.io.URLUtil
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.CopyValuesAction
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginCommonDataKeys.SELECTED_VALUES
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.extension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.nameWithoutExtension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toLowerCasePreservingASCIIRules
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.ChangeListener
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.ResetListener
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.FileDropHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.NotificationUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.createContextMenuMouseListener
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.createToggleAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.Unarchiver.OpenUnarchiverContext
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DropTarget
import java.awt.event.MouseEvent
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.event.HyperlinkEvent
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipMethod
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

class Unarchiver(
  private val project: Project?,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) :
  DeveloperUiTool(parentDisposable),
  DataProvider,
  ChangeListener,
  ResetListener,
  OpenDeveloperToolHandler<OpenUnarchiverContext> {
  // -- Properties ---------------------------------------------------------- //

  private val archiveTreeSortingMode =
    configuration.register("archiveTreeSortingMode", DEFAULT_SORTING_MODE)
  private val showArchiveNodeUncompressedSize =
    configuration.register("showArchiveNodeUncompressedSize", DEFAULT_SHOW_ARCHIVE_NODE_SIZE)
  private val showArchiveNodeTotalNumberOfChildren =
    configuration.register(
      "showArchiveNodeTotalNumberOfChildren",
      DEFAULT_SHOW_ARCHIVE_NODE_TOTAL_NUMBER_OF_CHILDREN,
    )
  private val openFileInEditorOnDoubleClick =
    configuration.register("openFileInEditorOnDoubleClick", DEFAULT_OPEN_IN_EDITOR_ON_DOUBLE_CLICK)
  private val lastSelectedOpenedDirectoryPath =
    configuration.register("lastSelectedOpenedDirectoryPath", DEFAULT_LAST_OPENED_DIRECTORY_PATH)
  private val lastSelectedTargetDirectoryPath =
    configuration.register(
      "lastSelectedTargetDirectoryPath",
      DEFAULT_LAST_SELECTED_TARGET_DIRECTORY_PATH,
    )
  private val createArchiveFilenameSubDirectory =
    configuration.register(
      "createArchiveFilenameSubDirectory",
      DEFAULT_CREATE_ARCHIVE_FILENAME_SUB_DIRECTORY,
    )
  private val clearTargetDirectory =
    configuration.register("clearTargetDirectory", DEFAULT_CLEAR_TARGET_DIRECTORY)
  private val createParentDirectories =
    configuration.register("createParentDirectories", DEFAULT_CREATE_PARENT_DIRECTORIES)
  private val preserveDirectoryStructure =
    configuration.register("preserveDirectoryStructure", DEFAULT_PRESERVE_DIRECTORY_STRUCTURE)
  private val preserveFileAttributes =
    configuration.register("preserveFileAttributes", DEFAULT_PRESERVE_FILE_ATTRIBUTES)
  private val openTargetDirectoryAfterExtraction =
    configuration.register(
      "openTargetDirectoryAfterExtraction",
      DEFAULT_OPEN_TARGET_DIRECTORY_AFTER_EXTRACTION,
    )

  private var tree: ArchiveTree? = null
  private val content = BorderLayoutPanel()
  private val noArchiveFilePanel by lazy { createNoArchiveFilePanel() }
  private val readingArchiveFilePanel by lazy { createReadingArchiveFilePanel() }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    content.dropTarget = DropTarget(content, FileDropHandler(project, openArchiveFile()))
    content.addToCenter(noArchiveFilePanel)

    row { cell(content).resizableColumn().align(Align.FILL) }.resizableRow()
  }

  override fun afterBuildUi() {
    configuration.addChangeListener(parentDisposable, this)
    configuration.addResetListener(parentDisposable, this)
  }

  override fun configurationReset() {
    closeArchiveFile()
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    tree?.let {
      val treeModel = it.tree.model as DefaultTreeModel

      if (property == archiveTreeSortingMode) {
        doSortArchiveTree((treeModel.root as DefaultMutableTreeNode).userObject as DirectoryNode)
      }

      val expandedPaths = TreeUtil.collectExpandedPaths(it.tree)
      treeModel.reload()
      it.searchModel.updateStructure()
      TreeUtil.restoreExpandedPaths(it.tree, expandedPaths)
    }
  }

  /**
   * This method may get called before the [tree] was initialized, for example, during a drop of a
   * file.
   */
  override fun getData(dataId: String): Any? =
    when {
      SELECTED_VALUES.`is`(dataId) -> tree?.getSelectedArchiveNodes()
      else -> super.getData(dataId)
    }

  override fun applyOpenDeveloperToolContext(context: OpenUnarchiverContext) {
    openArchiveFile().invoke(context.archiveFilePath)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun replaceContent(centerContent: JComponent) {
    content.removeAll()
    content.addToCenter(centerContent)
    content.revalidate()
    content.repaint()
  }

  private fun openArchiveFile(): (Path) -> Unit = { archiveFilePath ->
    lastSelectedOpenedDirectoryPath.set(archiveFilePath.parent.toString())

    val previousExpandedRelativePaths = mutableSetOf<String>()
    ApplicationManager.getApplication().invokeAndWait {
      tree?.let { tree ->
        TreeUtil.collectExpandedPaths(tree.tree)
          .mapNotNull { treePath -> ArchiveNode.findInLastPathComponent(treePath) }
          .forEach { previousExpandedRelativePaths.add(it.relativePath) }
      }

      replaceContent(readingArchiveFilePanel)
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val rootNode = buildArchiveTree(archiveFilePath)
        doSortArchiveTree(rootNode)

        ApplicationManager.getApplication().invokeLater {
          replaceContent(createArchiveTreePanel(archiveFilePath, rootNode))

          if (previousExpandedRelativePaths.isNotEmpty()) {
            TreeUtil.promiseExpand(tree!!.tree) { treePath ->
              val archiveNode = ArchiveNode.findInLastPathComponent(treePath)
              if (
                archiveNode != null &&
                  previousExpandedRelativePaths.contains(archiveNode.relativePath)
              ) {
                TreeVisitor.Action.CONTINUE
              } else {
                TreeVisitor.Action.SKIP_CHILDREN
              }
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Reading archive file failed", e)
        ApplicationManager.getApplication().invokeLater {
          replaceContent(noArchiveFilePanel)
          Messages.showErrorDialog(
            project,
            "Reading archive failed: ${e.message}",
            "Reading Archive Failed",
          )
        }
      }
    }
  }

  private fun buildArchiveTree(archiveFilePath: Path) =
    RootNode(archiveFilePath).apply {
      val normalizedPathToDirectoryNode = mutableMapOf<String, DirectoryNode>()
      normalizedPathToDirectoryNode[""] = this
      iterateEntries { archiveEntry, _ ->
        val path = archiveEntry.name
        val normalizedPath = path.trimEnd('/')
        val parentNode: DirectoryNode =
          buildParentDirectoryTreeStructure(this, normalizedPath, normalizedPathToDirectoryNode) {
            it.totalChildren += 1
            it.addUncompressedSize(archiveEntry.size)
          }
        val name = normalizedPath.substringAfterLast("/", normalizedPath)
        val isDirectory = path.endsWith("/")
        if (!isDirectory) {
          val fileNode = FileNode(name, archiveEntry)
          parentNode.children.add(fileNode)
        } else {
          if (!normalizedPathToDirectoryNode.containsKey(normalizedPath)) {
            val directoryNode = DirectoryNode(name, path, archiveEntry)
            normalizedPathToDirectoryNode[normalizedPath] = directoryNode
            parentNode.children.add(directoryNode)
          } else {
            normalizedPathToDirectoryNode[normalizedPath]!!.archiveEntry = archiveEntry
          }
        }
        true
      }
    }

  private fun buildParentDirectoryTreeStructure(
    rootNode: DirectoryNode,
    normalizedPath: String,
    normalizedPathToDirectoryNode: MutableMap<String, DirectoryNode>,
    modifyParentNode: (DirectoryNode) -> Unit,
  ): DirectoryNode {
    val parentPathParts = normalizedPath.split("/")
    var currentPath = ""
    var parentNode: DirectoryNode = rootNode
    for (i in parentPathParts.indices) {
      val currentNormalizedPath = currentPath.trimEnd('/')
      parentNode =
        normalizedPathToDirectoryNode.computeIfAbsent(currentNormalizedPath) {
          val directoryNode =
            DirectoryNode(
              currentNormalizedPath.substringAfterLast("/", currentNormalizedPath),
              currentPath,
              null,
            )
          parentNode.children.add(directoryNode)
          directoryNode
        }
      modifyParentNode(parentNode)
      currentPath += parentPathParts[i] + "/"
    }
    return parentNode
  }

  private fun doSortArchiveTree(node: DirectoryNode) {
    node.children.filterIsInstance<DirectoryNode>().forEach { doSortArchiveTree(it) }
    node.children.sortWith(archiveTreeSortingMode.get().comparator)
  }

  private fun createArchiveTreePanel(archiveFilePath: Path, rootNode: RootNode) = panel {
    val archiveTree = ArchiveTree(rootNode)
    tree = archiveTree

    row {
        cell(archiveTree.installSearchField()).gap(RightGap.SMALL)

        val actionGroup =
          DefaultActionGroup().apply {
            val commonActionsManager = CommonActionsManager.getInstance()
            add(commonActionsManager.createExpandAllHeaderAction(archiveTree.tree))
            add(commonActionsManager.createCollapseAllHeaderAction(archiveTree.tree))
            addSeparator()
            add(createSettingsActionGroup())
            add(createReloadAction(archiveFilePath))
            addSeparator()
            add(
              ChooseArchiveFileToOpenAction(
                project,
                archiveTree.tree,
                lastSelectedOpenedDirectoryPath,
                openArchiveFile(),
              )
            )
          }
        cell(
          ActionManager.getInstance()
            .createActionToolbar(Unarchiver::class.qualifiedName!!, actionGroup, true)
            .run {
              targetComponent = component
              component
            }
        )
      }
      .bottomGap(BottomGap.NONE)

    row {
        cell(ScrollPaneFactory.createScrollPane(archiveTree.component, false))
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
      .topGap(TopGap.NONE)
  }

  private fun createSettingsActionGroup() =
    object : DefaultActionGroup("Settings", true) {

      init {
        templatePresentation.icon = AllIcons.General.GearPlain

        add(
          DefaultActionGroup("Sorting", true).apply {
            SortingMode.entries.forEach {
              add(
                createToggleAction(
                  it.title,
                  { archiveTreeSortingMode.get() == it },
                  { state -> if (state) archiveTreeSortingMode.set(it) },
                )
              )
            }
          }
        )

        add(
          createToggleAction(
            "Show uncompressed size",
            { showArchiveNodeUncompressedSize.get() },
            { showArchiveNodeUncompressedSize.set(it) },
          )
        )
        add(
          createToggleAction(
            "Show total number of children",
            { showArchiveNodeTotalNumberOfChildren.get() },
            { showArchiveNodeTotalNumberOfChildren.set(it) },
          )
        )

        addSeparator()

        add(
          createToggleAction(
            "Open file in editor on double click",
            { openFileInEditorOnDoubleClick.get() },
            { openFileInEditorOnDoubleClick.set(it) },
          )
        )

        addSeparator()

        add(createCloseArchiveFileAction())
      }

      @Suppress("UnstableApiUsage")
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = UIUtil.isShowing(tree!!.tree)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

  private fun createReloadAction(archiveFilePath: Path): AnAction =
    object : DumbAwareAction("Reload", null, AllIcons.Actions.Refresh) {

      override fun actionPerformed(e: AnActionEvent) {
        openArchiveFile().invoke(archiveFilePath)
      }

      @Suppress("UnstableApiUsage")
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = UIUtil.isShowing(tree!!.tree)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

  private fun createCloseArchiveFileAction(): AnAction =
    object : DumbAwareAction("Close Archive File") {

      override fun actionPerformed(e: AnActionEvent) {
        closeArchiveFile()
      }

      @Suppress("UnstableApiUsage")
      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = UIUtil.isShowing(tree!!.tree)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

  private fun closeArchiveFile() {
    content.removeAll()
    content.addToCenter(noArchiveFilePanel)
    content.revalidate()
    content.repaint()
  }

  private fun createNoArchiveFilePanel(): JComponent {
    val actionsPanel = panel {
      row {
        cell(
          ChooseArchiveFileToOpenAction(
              project,
              null,
              lastSelectedOpenedDirectoryPath,
              openArchiveFile(),
            )
            .toHyperlinkLabel()
        )
      }
      row {
          cell(
            JBLabel(
              "Drop archive file here to open",
              AllIcons.Actions.Download,
              SwingConstants.LEFT,
            )
          )
        }
        .bottomGap(BottomGap.MEDIUM)
      row {
        lateinit var supportedTypesLink: JComponent
        supportedTypesLink =
          link("Supported archives and known limitations") {
              val text =
                """
            <p>Supported archive types: ${supportedArchiveExtensions.sorted().joinToString(", ")}</p>
            <p>The Apache Commons Compress 1.26.0 library is used for the underlining archive handling,<br>which has some <a href="https://commons.apache.org/proper/commons-compress/limitations.html">known limitations</a> (e.g., encryption is not supported).</p>
          """
                  .trimIndent()
              JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, null, UIUtil.getPanelBackground()) { e ->
                  if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.browse(e.url)
                  }
                }
                .setDialogMode(true)
                .setBorderColor(JBColor.border())
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(true)
                .setCloseButtonEnabled(true)
                .createBalloon()
                .apply {
                  setAnimationEnabled(false)
                  show(RelativePoint.getSouthOf(supportedTypesLink), Balloon.Position.below)
                }
            }
            .component
      }
    }
    return panel { row { cell(actionsPanel).align(Align.CENTER) }.resizableRow() }
  }

  private fun createReadingArchiveFilePanel() = panel {
    row {
        cell(JBLabel("Reading archive...").apply { icon = AnimatedIcon.Default.INSTANCE })
          .align(Align.CENTER)
      }
      .resizableRow()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private inner class ArchiveTree(private val rootNode: RootNode) :
    FilteringTree<DefaultMutableTreeNode, ArchiveNode>(Tree(), DefaultMutableTreeNode(rootNode)) {

    init {
      tree.cellRenderer =
        ArchiveTreeNodeRenderer(
          showArchiveNodeUncompressedSize = showArchiveNodeUncompressedSize,
          showArchiveNodeTotalNumberOfChildren = showArchiveNodeTotalNumberOfChildren,
        )

      tree.addMouseListener(
        createContextMenuMouseListener(Unarchiver::class.java.simpleName) { mouseEvent ->
          DefaultActionGroup().apply {
            add(createExtractAction())
            add(createFactExtractAction())

            addSeparator()

            add(createOpenInEditorAction())
            add(createOpenWithDefaultApplicationAction())
            add(createOpenEnclosingDirectoryAction())
            add(createCopyContentToClipboardAction())

            addSeparator()

            add(createShowSelectedElementDetailsAction(mouseEvent))

            addSeparator()

            add(
              DefaultActionGroup("Copy", true).apply {
                templatePresentation.icon = PlatformIcons.COPY_ICON
                add(
                  CopyValuesAction(
                    "Filename",
                    { "$it Filenames" },
                    { (it as ArchiveNode).fileName },
                    null,
                  )
                )
                add(
                  CopyValuesAction(
                    "Relative Path",
                    { "$it Relative Paths" },
                    { (it as ArchiveNode).relativePath()?.toString() },
                    null,
                  )
                )
                add(
                  CopyValuesAction(
                    "Absolute Path",
                    { "$it Absolute Paths" },
                    { (it as ArchiveNode).absolutePath(rootNode.archiveFilePath)?.toString() },
                    null,
                  )
                )
              }
            )
          }
        }
      )

      if (project != null) {
        object : DoubleClickListener() {

            override fun onDoubleClick(e: MouseEvent): Boolean {
              if (!openFileInEditorOnDoubleClick.get()) {
                return false
              }
              openInEditor(getSelectedArchiveNodes().filterIsInstance<FileNode>(), project, false)
              return true
            }
          }
          .installOn(tree)
      }
    }

    private fun createCopyContentToClipboardAction() =
      ArchiveNodeAction("Copy Content to Clipboard", { it is FileNode && it.isTextFile() }) {
        archiveNode,
        _ ->
        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            val content = rootNode.readEntry(archiveNode.archiveEntry!!)
            CopyPasteManager.getInstance()
              .setContents(StringSelection(content.toString(StandardCharsets.UTF_8)))
          } catch (e: Exception) {
            log.warn("Failed to read archive entry: ${archiveNode.archiveEntry!!.name}", e)
            ApplicationManager.getApplication().invokeLater {
              Messages.showErrorDialog(
                project,
                "Failed to read archive entry: ${e.message}",
                "Copy Content to Clipboard Failed",
              )
            }
          }
        }
      }

    override fun getNodeClass(): Class<out DefaultMutableTreeNode> =
      DefaultMutableTreeNode::class.java

    override fun createNode(archiveNode: ArchiveNode): DefaultMutableTreeNode =
      DefaultMutableTreeNode(archiveNode)

    override fun getChildren(archiveNode: ArchiveNode): Iterable<ArchiveNode> =
      if (archiveNode is DirectoryNode) archiveNode.children else emptyList()

    override fun getText(archiveNode: ArchiveNode?): String? = archiveNode?.fileName

    private fun createExtractAction() =
      ArchiveNodesAction("Extract...", { true }) { archiveNodes, _ ->
        ApplicationManager.getApplication().executeOnPooledThread {
          val archiveNodesToExtract = determineArchiveNodesToExtract(archiveNodes)
          if (archiveNodesToExtract.archiveNodes.isEmpty()) {
            log.info("No entries to extract found")
            return@executeOnPooledThread
          }

          ApplicationManager.getApplication().invokeLater {
            showExtractionDialog(rootNode, archiveNodesToExtract) { extractionContext ->
              ApplicationManager.getApplication().executeOnPooledThread {
                ExtractTask(project, extractionContext).queue()
              }
            }
          }
        }
      }

    private fun createFactExtractAction() =
      ArchiveNodesAction(
        "Fast Extract",
        { true },
        "Extracts the selected entry into a temporary directory.",
      ) { archiveNodes, _ ->
        ApplicationManager.getApplication().executeOnPooledThread {
          val archiveNodesToExtract = determineArchiveNodesToExtract(archiveNodes)
          if (archiveNodesToExtract.archiveNodes.isEmpty()) {
            log.info("No entries to extract found")
            return@executeOnPooledThread
          }

          val tempDirectory =
            Files.createTempDirectory("${rootNode.fileName.substringBeforeLast(".")}-")
          tempDirectory.toFile().deleteOnExit()

          ExtractTask(
              project = project,
              extractionContext =
                ExtractionContext(
                  rootNode = rootNode,
                  archiveNodes = archiveNodesToExtract.archiveNodes,
                  targetDirectoryPath = tempDirectory,
                  clearTargetDirectory = true,
                  preserveDirectoryStructure = true,
                  preserveFileAttributes = true,
                  createParentDirectories = false,
                  openTargetDirectoryAfterExtraction = true,
                ),
            )
            .queue()
        }
      }

    private fun createOpenEnclosingDirectoryAction() =
      ArchiveNodeAction("Open Enclosing Directory...", { it is RootNode }) { rootNode, _ ->
        BrowserUtil.browse((rootNode as RootNode).archiveFilePath.parent)
      }

    private fun createOpenInEditorAction() =
      ArchiveNodesAction("Open in Editor...", { it is FileNode }) { archiveNodes, e ->
        val project =
          e.dataContext.getData(CommonDataKeys.PROJECT)
            ?: throw IllegalStateException("snh: Data missing")
        openInEditor(archiveNodes.map { it as FileNode }, project, true)
      }

    private fun openInEditor(
      archiveNodes: List<FileNode>,
      project: Project,
      notifyOnError: Boolean,
    ) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFiles =
          archiveNodes.mapNotNull {
            virtualFileManager.findFileByUrl("jar://${it.absolutePath(rootNode.archiveFilePath)}")
          }
        ApplicationManager.getApplication().invokeLater {
          virtualFiles.forEach {
            val openEditor = fileEditorManager.openEditor(OpenFileDescriptor(project, it), true)
            if (openEditor.isEmpty() && notifyOnError) {
              Messages.showErrorDialog(
                project,
                "Unable to open file '${it.name}' in editor.",
                "Open in Editor Failed",
              )
            }
          }
        }
      }
    }

    private fun createOpenWithDefaultApplicationAction() =
      ArchiveNodeAction("Open With Default Application...", { it is RootNode }) { rootNode, _ ->
        BrowserUtil.browse((rootNode as RootNode).archiveFilePath)
      }

    private fun createShowSelectedElementDetailsAction(contextMenuMouseEvent: MouseEvent) =
      object : DumbAwareAction("Show Details...") {

        override fun update(e: AnActionEvent) {
          val selectedValues =
            SELECTED_VALUES.getData(e.dataContext)
              ?: throw IllegalStateException("snh: Data missing")

          e.presentation.isVisible =
            selectedValues.singleOrNull()?.let {
              it is RootNode || (it is ArchiveNode && it.archiveEntry != null)
            } == true
        }

        override fun actionPerformed(e: AnActionEvent) {
          val selectedValues =
            SELECTED_VALUES.getData(e.dataContext)
              ?: throw IllegalStateException("snh: Data missing")
          val archiveNode = selectedValues.singleOrNull()?.uncheckedCastTo(ArchiveNode::class)
          if (archiveNode !is RootNode && archiveNode?.archiveEntry == null) {
            return
          }

          val (title, panel) =
            when (archiveNode) {
              is RootNode ->
                archiveNode.archiveFilePath.fileName.toString() to
                  createArchiveFileDetails(archiveNode)
              else ->
                "${archiveNode.fileName} (${if (archiveNode is DirectoryNode) "directory" else "file"})" to
                  createArchiveEntryDetails(archiveNode)
            }
          JBPopupFactory.getInstance()
            .createBalloonBuilder(panel)
            .setTitle(title)
            .setDialogMode(true)
            .setFillColor(UIUtil.getPanelBackground())
            .setBorderColor(JBColor.border())
            .setBlockClicksThroughBalloon(true)
            .setRequestFocus(true)
            .setCloseButtonEnabled(true)
            .createBalloon()
            .apply {
              setAnimationEnabled(false)
              show(RelativePoint(contextMenuMouseEvent.locationOnScreen), Balloon.Position.below)
            }
        }

        private fun createArchiveEntryDetails(archiveNode: ArchiveNode) = panel {
          val archiveEntry = archiveNode.archiveEntry!!

          if (archiveNode is DirectoryNode) {
            row {
                label("Number of direct entries:")
                label(archiveNode.children.size.toString())
              }
              .layout(RowLayout.PARENT_GRID)
            row {
                label("Number of all entries:")
                label(archiveNode.totalChildren.toString())
              }
              .layout(RowLayout.PARENT_GRID)
          }

          row {
              label(
                "Uncompressed size${if (archiveNode is DirectoryNode) " of all entries" else ""}:"
              )
              label(StringUtil.formatFileSize(archiveNode.totalUncompressedSize() ?: 0))
            }
            .layout(RowLayout.PARENT_GRID)
            .bottomGap(BottomGap.NONE)
          if (archiveNode.inaccurateTotalUncompressedSize) {
            row {
                label(
                  "The uncompressed size may be inaccurate because some entries do not provide size information."
                )
              }
              .topGap(TopGap.NONE)
          }

          if (archiveEntry is ZipArchiveEntry) {
            if (archiveNode is FileNode) {
              row {
                  label("Compressed size:")
                  label(StringUtil.formatFileSize(archiveEntry.compressedSize))
                }
                .layout(RowLayout.PARENT_GRID)
            }

            row {
                label("Method:")
                val method = ZipMethod.getMethodByCode(archiveEntry.method)
                label(method.name.split("_").joinToString(" "))
              }
              .layout(RowLayout.PARENT_GRID)
          }

          val fileTimes = FileTimes.fromArchiveEntry(archiveEntry)
          row {
              label("Creation time:")
              label(
                if (fileTimes.creationTime != null)
                  DateFormatUtil.formatDateTime(fileTimes.creationTime.toMillis())
                else "Unknown"
              )
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Last modified time:")
              label(
                if (fileTimes.lastModifiedTime != null)
                  DateFormatUtil.formatDateTime(fileTimes.lastModifiedTime.toMillis())
                else "Unknown"
              )
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Last access time:")
              label(
                if (fileTimes.lastAccessTime != null)
                  DateFormatUtil.formatDateTime(fileTimes.lastAccessTime.toMillis())
                else "Unknown"
              )
            }
            .layout(RowLayout.PARENT_GRID)

          if (archiveEntry is ZipEntry) {
            row {
                label("Comment:")
                label(archiveEntry.comment)
              }
              .layout(RowLayout.PARENT_GRID)
          }

          if (archiveEntry is ZipEntry && archiveEntry.extra != null) {
            row {
                label("Extra data size:")
                label(StringUtil.formatFileSize(archiveEntry.extra.size.toLong()))
              }
              .layout(RowLayout.PARENT_GRID)
          }
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
      }

    private fun createArchiveFileDetails(rootNode: RootNode) =
      try {
        panel {
          row {
              label("Number of direct entries:")
              label(rootNode.children.size.toString())
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Number of all entries:")
              label(rootNode.totalChildren.toString())
            }
            .layout(RowLayout.PARENT_GRID)
          val zipFileAttributes =
            Files.readAttributes(rootNode.archiveFilePath, BasicFileAttributes::class.java)
          row {
              label("Actual size on disk:")
              label(FileUtils.byteCountToDisplaySize(zipFileAttributes.size()))
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Uncompressed size of all entries:")
              label(StringUtil.formatFileSize(rootNode.totalUncompressedSize() ?: 0))
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Creation time:")
              label(DateFormatUtil.formatDateTime(zipFileAttributes.creationTime().toMillis()))
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Last modified time:")
              label(DateFormatUtil.formatDateTime(zipFileAttributes.lastModifiedTime().toMillis()))
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Last access time:")
              label(DateFormatUtil.formatDateTime(zipFileAttributes.lastAccessTime().toMillis()))
            }
            .layout(RowLayout.PARENT_GRID)
          row {
              label("Owner:")
              label(Files.getOwner(rootNode.archiveFilePath).name)
            }
            .layout(RowLayout.PARENT_GRID)
          row {
            val writable = Files.isWritable(rootNode.archiveFilePath)
            icon(if (writable) AllIcons.Ide.Readwrite else AllIcons.Ide.Readonly)
              .gap(RightGap.SMALL)
            label(if (writable) "File is writeable" else "File is readonly")
          }
        }
      } catch (e: IOException) {
        log.warn("Failed to read parameters of file: ${rootNode.archiveFilePath}", e)
        panel { row { label("Failed to read file attributes: ${e.message}").bold() } }
      }

    fun getSelectedArchiveNodes(): List<ArchiveNode> =
      TreeUtil.collectSelectedPaths(tree)
        .mapNotNull {
          it.lastPathComponent
            ?.uncheckedCastTo(DefaultMutableTreeNode::class)
            ?.userObject
            ?.uncheckedCastTo(ArchiveNode::class)
        }
        .toList()

    private fun showExtractionDialog(
      rootNode: RootNode,
      archiveNodesToExtract: ArchiveNodesToExtract,
      okCallback: (ExtractionContext) -> Unit,
    ) {
      object : DialogWrapper(project, tree, false, IdeModalityType.IDE) {

          init {
            title = "Extract"
            setSize(600, 400)
            isModal = true
            setOKButtonText("Extract")
            init()
          }

          override fun doOKAction() {
            okCallback(
              ExtractionContext(
                rootNode = rootNode,
                archiveNodes = archiveNodesToExtract.archiveNodes,
                targetDirectoryPath =
                  if (createArchiveFilenameSubDirectory.get()) {
                    Paths.get(lastSelectedTargetDirectoryPath.get())
                      .resolve(rootNode.archiveFilePath.nameWithoutExtension())
                  } else {
                    Paths.get(lastSelectedTargetDirectoryPath.get())
                  },
                clearTargetDirectory = clearTargetDirectory.get(),
                preserveDirectoryStructure = preserveDirectoryStructure.get(),
                preserveFileAttributes = preserveFileAttributes.get(),
                createParentDirectories = createParentDirectories.get(),
                openTargetDirectoryAfterExtraction = openTargetDirectoryAfterExtraction.get(),
              )
            )
            super.doOKAction()
          }

          override fun createCenterPanel(): JComponent = panel {
            row {
              textFieldWithBrowseButton(
                  FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle("Select Target Directory"),
                  project,
                )
                .label("Target directory:")
                .bindText(lastSelectedTargetDirectoryPath)
                .resizableColumn()
                .align(Align.FILL)
            }
            row {
              checkBox(
                  "Create a sub-directory with the archive name '${rootNode.archiveFilePath.nameWithoutExtension()}'"
                )
                .bindSelected(createArchiveFilenameSubDirectory)
            }
            row { checkBox("Clear target directory").bindSelected(clearTargetDirectory) }
              .bottomGap(BottomGap.SMALL)

            row {
              checkBox("Preserve directory structure")
                .bindSelected(preserveDirectoryStructure)
                .gap(RightGap.SMALL)
              contextHelp(
                "If unselected, all files will be extracted as a flat list, ignoring the directory structure."
              )
            }
            row {
              checkBox("Create parent directories")
                .bindSelected(createParentDirectories)
                .enabledIf(preserveDirectoryStructure)
                .gap(RightGap.SMALL)
              contextHelp(
                "<html>For example, if selected, for the path <code>first/second/third/</code> both parent directories <code>first/second/</code> will be created, otherwise only the directory <code>third/</code>.</html>"
              )
            }
            row { checkBox("Preserve file attributes").bindSelected(preserveFileAttributes) }
              .bottomGap(BottomGap.SMALL)

            row {
                checkBox("Open target directory after extraction")
                  .bindSelected(openTargetDirectoryAfterExtraction)
              }
              .bottomGap(BottomGap.SMALL)

            row {
                val entriesToExtractTable =
                  JBTable(
                    DefaultTableModel(
                      archiveNodesToExtract.displayPathsWithUncompressedSize
                        .map { (displayPath, totalUncompressedSize) ->
                          arrayOf(
                            displayPath,
                            if (totalUncompressedSize != null)
                              StringUtil.formatFileSize(totalUncompressedSize)
                            else "Unknown",
                          )
                        }
                        .toTypedArray(),
                      arrayOf("Path", "Uncompressed Size"),
                    )
                  )
                cell(ScrollPaneFactory.createScrollPane(entriesToExtractTable))
                  .resizableColumn()
                  .align(Align.FILL)
                  .label("Entries to extract:", LabelPosition.TOP)
              }
              .resizableRow()
              .bottomGap(BottomGap.NONE)
            row {
                comment(
                  "Expected size on disk: ${if (archiveNodesToExtract.inaccurateTotalUncompressedSize) "+" else ""}${
                StringUtil.formatFileSize(
                  archiveNodesToExtract.totalUncompressedSize
                )
              }"
                )
              }
              .topGap(TopGap.SMALL)
          }
        }
        .show()
    }

    private fun determineArchiveNodesToExtract(
      archiveNodes: List<ArchiveNode>
    ): ArchiveNodesToExtract {
      val rootNode = archiveNodes.find { it is RootNode }
      return if (rootNode != null) {
        ArchiveNodesToExtract(
          archiveNodes = listOf(rootNode),
          displayPathsWithUncompressedSize =
            (rootNode as RootNode).children.associate {
              it.relativePath to it.totalUncompressedSize()
            },
          totalUncompressedSize = rootNode.totalUncompressedSize() ?: 0,
          inaccurateTotalUncompressedSize = rootNode.inaccurateTotalUncompressedSize,
        )
      } else {
        val finalArchiveNodes = mutableListOf<ArchiveNode>()
        val displayPathsWithUncompressedSize = mutableMapOf<String, Long?>()
        val relativePathToArchiveNode = archiveNodes.associateBy { it.relativePath }.toSortedMap()
        var previousRelativePath: String? = null
        var totalUncompressedSize = 0L
        var inaccurateTotalUncompressedSize = false
        for ((relativePath, archiveNode) in relativePathToArchiveNode) {
          if (previousRelativePath == null || !relativePath.startsWith(previousRelativePath)) {
            finalArchiveNodes.add(archiveNode)
            val totalUncompressedSize0 = archiveNode.totalUncompressedSize()
            displayPathsWithUncompressedSize[archiveNode.relativePath] = totalUncompressedSize0
            if (totalUncompressedSize0 != null) {
              totalUncompressedSize += totalUncompressedSize0
            } else {
              inaccurateTotalUncompressedSize = true
            }
            previousRelativePath = relativePath
          }
        }
        ArchiveNodesToExtract(
          archiveNodes = finalArchiveNodes,
          displayPathsWithUncompressedSize = displayPathsWithUncompressedSize,
          totalUncompressedSize = totalUncompressedSize,
          inaccurateTotalUncompressedSize = inaccurateTotalUncompressedSize,
        )
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class FileTimes(
    val lastModifiedTime: FileTime?,
    val lastAccessTime: FileTime?,
    val creationTime: FileTime?,
  ) {

    companion object {

      fun fromArchiveEntry(archiveEntry: ArchiveEntry): FileTimes {
        val (lastModifiedTime, lastAccessTime, creationTime) =
          when (archiveEntry) {
            is ZipEntry ->
              Triple(
                archiveEntry.lastModifiedTime,
                archiveEntry.lastAccessTime,
                archiveEntry.creationTime,
              )
            is SevenZArchiveEntry ->
              Triple(
                if (archiveEntry.hasLastModifiedDate) archiveEntry.lastModifiedTime else null,
                if (archiveEntry.hasAccessDate) archiveEntry.accessTime else null,
                if (archiveEntry.hasCreationDate) archiveEntry.creationTime else null,
              )

            else ->
              Triple(
                archiveEntry.lastModifiedDate?.toInstant()?.let { FileTime.from(it) },
                null,
                null,
              )
          }
        return FileTimes(lastModifiedTime, lastAccessTime, creationTime)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ArchiveNodesAction(
    @NlsActions.ActionText title: String,
    private val filter: (Any) -> Boolean,
    @NlsActions.ActionDescription description: String? = null,
    private val actionPerformed: (List<ArchiveNode>, AnActionEvent) -> Unit,
  ) : DumbAwareAction(title, description, null) {

    override fun update(e: AnActionEvent) {
      val selectedValues =
        SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")

      e.presentation.isVisible = selectedValues.any(filter)
    }

    override fun actionPerformed(e: AnActionEvent) {
      val selectedValues =
        SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
      val archiveNodes = selectedValues.filter(filter).map { it as ArchiveNode }.toList()
      actionPerformed(archiveNodes, e)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ArchiveNodeAction(
    @NlsActions.ActionText title: String,
    private val filter: (ArchiveNode) -> Boolean,
    private val actionPerformed: (ArchiveNode, AnActionEvent) -> Unit,
  ) : DumbAwareAction(title) {

    override fun update(e: AnActionEvent) {
      val archiveNode = getSelectedArchiveNode(e)
      e.presentation.isVisible = archiveNode != null
    }

    override fun actionPerformed(e: AnActionEvent) {
      val archiveNode = getSelectedArchiveNode(e) ?: return
      actionPerformed(archiveNode, e)
    }

    private fun getSelectedArchiveNode(e: AnActionEvent): ArchiveNode? {
      val selectedValues =
        SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
      return selectedValues.singleOrNull()?.uncheckedCastTo(ArchiveNode::class)?.takeIf(filter)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
  }

  // -- Inner Type ---------------------------------------------------------- //

  private abstract class ArchiveNode(
    val fileName: String,
    val icon: Icon,
    private var totalUncompressedSize: Long = 0,
    /**
     * Must be a non-normalized path to distinguish between a directory and a file with the same
     * name.
     */
    val relativePath: String,
    var archiveEntry: ArchiveEntry?,
  ) {

    var inaccurateTotalUncompressedSize = false
      private set

    fun addUncompressedSize(size: Long) {
      if (size != ArchiveEntry.SIZE_UNKNOWN) {
        totalUncompressedSize += size
      } else {
        inaccurateTotalUncompressedSize = true
      }
    }

    fun totalUncompressedSize(): Long? =
      if (totalUncompressedSize != ArchiveEntry.SIZE_UNKNOWN) totalUncompressedSize else null

    override fun toString(): String = fileName

    open fun relativePath(): Path? = archiveEntry?.name?.let { Paths.get(it) }

    open fun absolutePath(archiveFilePath: Path): Path? {
      if (archiveEntry == null) {
        return null
      }

      return archiveFilePath.parent
        .resolve("${archiveFilePath.fileName}!")
        .resolve(Paths.get(archiveEntry!!.name))
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is ArchiveNode) return false

      if (relativePath != other.relativePath) return false

      return true
    }

    override fun hashCode(): Int = relativePath.hashCode()

    companion object {

      fun toRelativePath(path: String) =
        if (path.startsWith("/")) path.substringAfter("/") else path

      fun findInLastPathComponent(treePath: TreePath): ArchiveNode? =
        treePath.lastPathComponent
          ?.safeCastTo<DefaultMutableTreeNode>()
          ?.userObject
          ?.safeCastTo<ArchiveNode>()
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class FileNode(fileName: String, archiveEntry: ArchiveEntry) :
    ArchiveNode(
      fileName,
      determineIcon(fileName),
      archiveEntry.size,
      toRelativePath(archiveEntry.name),
      archiveEntry,
    ) {

    override fun relativePath(): Path = super.relativePath()!!

    override fun absolutePath(archiveFilePath: Path): Path = super.absolutePath(archiveFilePath)!!

    fun isTextFile() =
      textFileNames.contains(fileName) ||
        textFileExtensions.contains(
          fileName.substringAfterLast(".").toLowerCasePreservingASCIIRules()
        )

    companion object {

      private fun determineIcon(fileName: String): Icon {
        when (fileName) {
          ".htaccess" -> return AllIcons.FileTypes.Htaccess
          "config",
          "init",
          ".gitignore",
          ".gitattributes",
          ".editorconfig",
          "Dockerfile",
          "index",
          "Makefile" -> return AllIcons.FileTypes.Config
          "README",
          "LICENSE",
          "NOTICE",
          "CHANGELOG" -> AllIcons.FileTypes.Text
        }

        return when (fileName.substringAfterLast(".").toLowerCasePreservingASCIIRules()) {
          "mf" -> AllIcons.FileTypes.Manifest
          "zip",
          "tar",
          "jar" -> AllIcons.FileTypes.Archive
          "as" -> AllIcons.FileTypes.AS
          "aj" -> AllIcons.FileTypes.Aspectj
          "config",
          "conf",
          "cfg",
          "ini" -> AllIcons.FileTypes.Config
          "css",
          "scss",
          "sass" -> AllIcons.FileTypes.Css
          "dtd" -> AllIcons.FileTypes.Dtd
          "hprof" -> AllIcons.FileTypes.Hprof
          "html",
          "htm" -> AllIcons.FileTypes.Html
          "xhtml" -> AllIcons.FileTypes.Xhtml
          "xml",
          "pom" -> AllIcons.FileTypes.Xml
          "xsd" -> AllIcons.FileTypes.XsdFile
          "jpg",
          "jpeg",
          "png",
          "gif",
          "bmp",
          "tiff",
          "svg",
          "psd",
          "ai",
          "eps",
          "raw",
          "webp",
          "pdf",
          "ico" -> AllIcons.FileTypes.Image
          "java",
          "kt",
          "kts",
          "groovy",
          "scala" -> AllIcons.FileTypes.Java
          "class",
          "jmod" -> AllIcons.FileTypes.JavaClass
          "js" -> AllIcons.FileTypes.JavaScript
          "jfr" -> AllIcons.FileTypes.Jfr
          "json" -> AllIcons.FileTypes.Json
          "yaml",
          "yml" -> AllIcons.FileTypes.Yaml
          "jsp" -> AllIcons.FileTypes.Jsp
          "jspx" -> AllIcons.FileTypes.Jspx
          "properties" -> AllIcons.FileTypes.Properties
          "txt",
          "text",
          "md",
          "log",
          "sql",
          "csv",
          "tex" -> AllIcons.FileTypes.Text
          else -> AllIcons.FileTypes.Any_type
        }
      }
    }

    private val textFileNames =
      setOf(
        "config",
        "init",
        ".gitignore",
        ".gitattributes",
        ".editorconfig",
        "Dockerfile",
        "index",
        "Makefile",
        "README",
        "LICENSE",
        "NOTICE",
        "CHANGELOG",
        ".htaccess",
      )

    private val textFileExtensions =
      setOf(
        "txt",
        "html",
        "xml",
        "csv",
        "json",
        "log",
        "md",
        "css",
        "js",
        "php",
        "py",
        "java",
        "c",
        "cpp",
        "rb",
        "sql",
        "yml",
        "yaml",
        "ini",
        "conf",
        "cfg",
        "xml",
        "htm",
        "cs",
        "scala",
        "groovy",
        "sh",
        "bat",
        "ps1",
        "awk",
        "sed",
        "tex",
        "r",
        "scss",
        "less",
        "styl",
        "asp",
        "jsp",
        "aspx",
        "xhtml",
        "cfc",
        "cfm",
        "tpl",
        "twig",
        "handlebars",
        "mustache",
        "jsx",
        "tsx",
        "kt",
        "kts",
        "mf",
        "config",
        "properties",
        "pom",
        "text",
        "sass",
      )
  }

  // -- Inner Type ---------------------------------------------------------- //

  private open class DirectoryNode(
    name: String,
    relativePath: String,
    archiveEntry: ArchiveEntry?,
    icon: Icon = AllIcons.Nodes.Folder,
  ) : ArchiveNode(name, icon, 0, toRelativePath(relativePath), archiveEntry) {

    var totalChildren: Int = 0
    val children: MutableList<ArchiveNode> = mutableListOf()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class RootNode(val archiveFilePath: Path) :
    DirectoryNode(archiveFilePath.fileName.toString(), "", null, AllIcons.FileTypes.Archive) {

    override fun relativePath(): Path? = null

    override fun absolutePath(archiveFilePath: Path): Path = archiveFilePath

    fun iterateEntries(visitor: (ArchiveEntry, () -> InputStream) -> Boolean) {
      if (archiveFilePath.fileName.extension() == "7z") {
        val sevenZFile = SevenZFile.Builder().setFile(archiveFilePath.toFile()).get()
        var archiveEntry = sevenZFile.nextEntry
        while (archiveEntry != null) {
          if (!visitor(archiveEntry) { sevenZFile.getInputStream(archiveEntry) }) {
            break
          }
          archiveEntry = sevenZFile.nextEntry
        }
      } else {
        var archiveFileInputStream = BufferedInputStream(FileInputStream(archiveFilePath.toFile()))
        if (GzipUtils.isCompressedFileName(archiveFilePath.fileName.toString())) {
          archiveFileInputStream =
            BufferedInputStream(GzipCompressorInputStream(archiveFileInputStream))
        }

        val archiveInputStream: ArchiveInputStream<*> =
          ArchiveStreamFactory().createArchiveInputStream(archiveFileInputStream)
        archiveInputStream.use { archiveInputStream0 ->
          var archiveEntry = archiveInputStream0.nextEntry
          while (archiveEntry != null) {
            if (!visitor(archiveEntry) { archiveInputStream0 }) {
              break
            }
            archiveEntry = archiveInputStream0.nextEntry
          }
        }
      }
    }

    fun readEntry(archiveEntry: ArchiveEntry): ByteArray {
      var result: ByteArray? = null
      iterateEntries { archiveEntry0, archiveInputStream ->
        if (archiveEntry0 == archiveEntry) {
          result = IOUtils.toByteArray(archiveInputStream())
          false
        } else {
          true
        }
      }
      return result
        ?: throw IllegalStateException("Unable to find archive entry: ${archiveEntry.name}")
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ChooseArchiveFileToOpenAction(
    private val project: Project?,
    private val tree: Tree?,
    private val lastSelectedOpenedDirectoryPath: ValueProperty<String>,
    private val openArchiveCallback: (Path) -> Unit,
  ) : DumbAwareAction("Open Archive File", null, AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
      openArchiveDialog()
    }

    @Suppress("UnstableApiUsage")
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = tree != null && UIUtil.isShowing(tree)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    private fun openArchiveDialog() {
      ApplicationManager.getApplication().executeOnPooledThread {
        val startPath =
          VirtualFileManager.getInstance().findFileByUrl(lastSelectedOpenedDirectoryPath.get())

        ApplicationManager.getApplication().invokeLater {
          val descriptor =
            FileChooserDescriptorFactory.createSingleFileDescriptor()
              .withTitle("Open Archive File")
              .withExtensionFilter("Archive files", *supportedArchiveExtensions)
          val fileToOpen = FileChooser.chooseFile(descriptor, project, startPath)
          if (fileToOpen != null) {
            openArchiveCallback(fileToOpen.toNioPath())
          }
        }
      }
    }

    @Suppress("DialogTitleCapitalization")
    fun toHyperlinkLabel() =
      HyperlinkLabel(templatePresentation.text).apply {
        icon = templatePresentation.icon

        addHyperlinkListener { openArchiveDialog() }
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class OpenArchiveFileInUnarchiverAction :
    DumbAwareAction("Unarchiver", "Open archive file in the developer tool 'Unarchiver'.", null) {

    private val supportedArchiveExtensions = Companion.supportedArchiveExtensions.toSet()

    override fun update(e: AnActionEvent) {
      val selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
      e.presentation.isVisible = isSelectedFileSupported(selectedFiles)
    }

    override fun actionPerformed(e: AnActionEvent) {
      val selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
      if (!isSelectedFileSupported(selectedFiles)) {
        return
      }

      var sanitizedFilePath = selectedFiles!![0].path
      if (
        selectedFiles[0].fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL &&
          sanitizedFilePath.endsWith(URLUtil.JAR_SEPARATOR)
      ) {
        sanitizedFilePath = sanitizedFilePath.substringBeforeLast(URLUtil.JAR_SEPARATOR)
      }

      e.project
        ?.service<OpenDeveloperToolService>()
        ?.openTool(OpenUnarchiverContext(Paths.get(sanitizedFilePath)), openUnarchiverReference)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun isSelectedFileSupported(selectedFiles: Array<out VirtualFile>?) =
      selectedFiles?.size == 1 &&
        selectedFiles.any {
          it.extension != null && supportedArchiveExtensions.contains(it.extension)
        }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class SortingMode(val title: String, val comparator: Comparator<ArchiveNode>) {

    UNCOMPRESSED_SIZE_ASC(
      "Uncompressed size (ascending)",
      { a, b -> compareValues(a.totalUncompressedSize(), b.totalUncompressedSize()) },
    ),
    UNCOMPRESSED_SIZE_DESC(
      "Uncompressed size (descending)",
      { a, b -> compareValues(b.totalUncompressedSize(), a.totalUncompressedSize()) },
    ),
    FILENAME_ASC("Filename (ascending)", { a, b -> a.fileName.compareTo(b.fileName) }),
    FILENAME_DESC("Filename (descending)", { a, b -> b.fileName.compareTo(a.fileName) }),
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ArchiveTreeNodeRenderer(
    val showArchiveNodeUncompressedSize: ValueProperty<Boolean>,
    val showArchiveNodeTotalNumberOfChildren: ValueProperty<Boolean>,
  ) : NodeRenderer() {

    override fun customizeCellRenderer(
      tree: JTree,
      value: Any,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean,
    ) {
      val archiveNode =
        value
          .uncheckedCastTo(DefaultMutableTreeNode::class)
          .userObject
          .uncheckedCastTo(ArchiveNode::class)

      icon = archiveNode.icon
      append(archiveNode.fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES)

      val metaInformation = mutableListOf<String>()
      if (showArchiveNodeUncompressedSize.get()) {
        archiveNode.totalUncompressedSize()?.let {
          if (!(it == 0L && archiveNode.inaccurateTotalUncompressedSize)) {
            metaInformation.add(
              "${if (archiveNode.inaccurateTotalUncompressedSize) "~" else ""}${StringUtil.formatFileSize(it)}"
            )
          }
        }
      }
      if (showArchiveNodeTotalNumberOfChildren.get() && archiveNode is DirectoryNode) {
        metaInformation.add("${archiveNode.totalChildren} entries")
      }
      if (metaInformation.isNotEmpty()) {
        append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        append(metaInformation.joinToString(", "), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class ExtractionContext(
    val rootNode: RootNode,
    val archiveNodes: List<ArchiveNode>,
    val targetDirectoryPath: Path,
    val clearTargetDirectory: Boolean,
    val createParentDirectories: Boolean,
    val preserveDirectoryStructure: Boolean,
    val preserveFileAttributes: Boolean,
    val openTargetDirectoryAfterExtraction: Boolean,
  )

  // -- Inner Type ---------------------------------------------------------- //

  private data class ArchiveNodesToExtract(
    val archiveNodes: List<ArchiveNode>,
    val displayPathsWithUncompressedSize: Map<String, Long?>,
    val totalUncompressedSize: Long,
    val inaccurateTotalUncompressedSize: Boolean,
  )

  // -- Inner Type ---------------------------------------------------------- //

  private class ExtractTask(project: Project?, private val extractionContext: ExtractionContext) :
    Task.ConditionalModal(
      project,
      "Extracting ${extractionContext.rootNode.fileName}",
      true,
      DEAF,
    ) {

    override fun run(progressIndicator: ProgressIndicator) {
      progressIndicator.checkCanceled()
      progressIndicator.text = "Preparing directory structure..."

      if (!Files.exists(extractionContext.targetDirectoryPath)) {
        Files.createDirectories(extractionContext.targetDirectoryPath)
      } else {
        if (!Files.isDirectory(extractionContext.targetDirectoryPath)) {
          throw IllegalArgumentException(
            "The target path '${extractionContext.targetDirectoryPath}' already exists but it is not a directory"
          )
        } else if (extractionContext.clearTargetDirectory) {
          FileUtils.cleanDirectory(extractionContext.targetDirectoryPath.toFile())
        }
      }

      // Prepare directories...
      val preparedFileNodeExtractions = mutableMapOf<String, Path>()
      extractionContext.archiveNodes.forEach { archiveNode ->
        progressIndicator.checkCanceled()
        progressIndicator.text2 = archiveNode.relativePath

        when (archiveNode) {
          is DirectoryNode ->
            preparedFileNodeExtractions.putAll(
              prepareDirectoryNode(
                archiveNode,
                extractionContext.targetDirectoryPath,
                extractionContext,
              )
            )

          is FileNode ->
            prepareStandaloneFileNodeExtraction(archiveNode, extractionContext).let {
              (archiveEntry, path) ->
              preparedFileNodeExtractions[archiveEntry.name] = path
            }

          else ->
            throw IllegalStateException(
              "Unknown archive node type: ${archiveNode::class.qualifiedName}"
            )
        }
      }

      // Copy files...
      val numOfFileNodesToExtract = preparedFileNodeExtractions.size
      val archiveEntryToTargetFilePathToCopy = preparedFileNodeExtractions.toMutableMap()
      extractionContext.rootNode.iterateEntries { archiveEntry, archiveInputStream ->
        if (archiveEntryToTargetFilePathToCopy.containsKey(archiveEntry.name)) {
          progressIndicator.checkCanceled()
          progressIndicator.text =
            "Extracting $numOfFileNodesToExtract file${if (numOfFileNodesToExtract == 1) "s" else ""} (${archiveEntryToTargetFilePathToCopy.size} remaining)..."
          progressIndicator.text2 = archiveEntry.name
          progressIndicator.fraction =
            (numOfFileNodesToExtract - archiveEntryToTargetFilePathToCopy.size.toDouble()) /
              numOfFileNodesToExtract

          val targetPath = archiveEntryToTargetFilePathToCopy[archiveEntry.name]!!
          Files.newOutputStream(targetPath, WRITE, CREATE_NEW, TRUNCATE_EXISTING).use { outputStream
            ->
            IOUtils.copy(archiveInputStream(), outputStream)
          }
          archiveEntryToTargetFilePathToCopy.remove(archiveEntry.name)
          if (extractionContext.preserveFileAttributes) {
            restoreFileAttributes(archiveEntry, targetPath)
          }
        }
        archiveEntryToTargetFilePathToCopy.isNotEmpty()
      }
      if (archiveEntryToTargetFilePathToCopy.isNotEmpty()) {
        throw IllegalStateException(
          "Unable to find archive entries: ${archiveEntryToTargetFilePathToCopy.map { it.key }.joinToString(", ")}"
        )
      }
    }

    override fun onThrowable(error: Throwable) {
      log.warn("Extraction failed", error)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(project, error.message, "Extraction Failed")
      }
    }

    override fun onSuccess() {
      if (extractionContext.openTargetDirectoryAfterExtraction) {
        BrowserUtil.browse(extractionContext.targetDirectoryPath)
      } else {
        notifyAboutExtractionResult(extractionContext)
      }
    }

    private fun prepareDirectoryNode(
      directoryNode: DirectoryNode,
      baseDirectoryPath: Path,
      extractionContext: ExtractionContext,
    ): Map<String, Path> {
      val directoryPath =
        if (extractionContext.preserveDirectoryStructure) {
          val relativeDirectoryPath =
            if (extractionContext.createParentDirectories) {
              Paths.get(directoryNode.relativePath)
            } else {
              Path.of(directoryNode.fileName)
            }
          val directoryPath =
            Files.createDirectories(baseDirectoryPath.resolve(relativeDirectoryPath))
          if (extractionContext.preserveFileAttributes && directoryNode.archiveEntry != null) {
            restoreFileAttributes(directoryNode.archiveEntry!!, directoryPath)
          }
          directoryPath
        } else {
          baseDirectoryPath
        }

      val preparedFileNodeExtractions = mutableMapOf<String, Path>()
      directoryNode.children.forEach { archiveNode ->
        when (archiveNode) {
          is DirectoryNode ->
            preparedFileNodeExtractions.putAll(
              prepareDirectoryNode(archiveNode, directoryPath, extractionContext)
            )

          is FileNode -> {
            val filePath = directoryPath.resolve(archiveNode.fileName)
            preparedFileNodeExtractions[archiveNode.archiveEntry!!.name] = filePath
          }

          else ->
            throw IllegalStateException(
              "Unknown archive node type: ${archiveNode::class.qualifiedName}"
            )
        }
      }
      return preparedFileNodeExtractions
    }

    /**
     * A [FileNode] is "standalone" if its parent [DirectoryNode] is not extracted. This method
     * creates the associated directory structure mmit.
     */
    private fun prepareStandaloneFileNodeExtraction(
      fileNode: FileNode,
      extractionContext: ExtractionContext,
    ): Pair<ArchiveEntry, Path> {
      val filePath =
        if (
          extractionContext.preserveDirectoryStructure && extractionContext.createParentDirectories
        ) {
          val targetPath =
            extractionContext.targetDirectoryPath.resolve(Paths.get(fileNode.relativePath))
          Files.createDirectories(targetPath.parent)
          targetPath
        } else {
          extractionContext.targetDirectoryPath.resolve(Paths.get(fileNode.fileName))
        }

      return fileNode.archiveEntry!! to filePath
    }

    private fun notifyAboutExtractionResult(extractionContext: ExtractionContext) {
      val numOfExtractedEntries = extractionContext.archiveNodes.size
      NotificationUtils.notifyOnToolWindow(
        message =
          "$numOfExtractedEntries ${if (numOfExtractedEntries == 1) "entry" else "entries"} have been extracted to: ${extractionContext.targetDirectoryPath}",
        project = project,
        notificationType = NotificationType.INFORMATION,
        object : DumbAwareAction("Open Target Directory") {

          override fun actionPerformed(e: AnActionEvent) {
            BrowserUtil.browse(extractionContext.targetDirectoryPath)
          }
        },
      )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class OpenUnarchiverContext(val archiveFilePath: Path) : OpenDeveloperToolContext

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<Unarchiver> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "Unarchiver", contentTitle = CONTENT_TITLE)

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Unarchiver) = {
      assert(ID == context.id)
      Unarchiver(project, it, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val ID = "unarchiver"
    private const val CONTENT_TITLE = "Unarchiver"

    private val DEFAULT_SORTING_MODE = SortingMode.FILENAME_ASC
    private const val DEFAULT_SHOW_ARCHIVE_NODE_SIZE = true
    private const val DEFAULT_SHOW_ARCHIVE_NODE_TOTAL_NUMBER_OF_CHILDREN = true
    private const val DEFAULT_OPEN_IN_EDITOR_ON_DOUBLE_CLICK = true
    private val DEFAULT_LAST_OPENED_DIRECTORY_PATH = SystemProperties.getUserHome()
    private val DEFAULT_LAST_SELECTED_TARGET_DIRECTORY_PATH = SystemProperties.getUserHome()
    private const val DEFAULT_CREATE_ARCHIVE_FILENAME_SUB_DIRECTORY = true
    private const val DEFAULT_CLEAR_TARGET_DIRECTORY = false
    private const val DEFAULT_PRESERVE_DIRECTORY_STRUCTURE = true
    private const val DEFAULT_CREATE_PARENT_DIRECTORIES = true
    private const val DEFAULT_PRESERVE_FILE_ATTRIBUTES = true
    private const val DEFAULT_OPEN_TARGET_DIRECTORY_AFTER_EXTRACTION = true

    private val log = logger<Unarchiver>()

    val openUnarchiverReference = OpenDeveloperToolReference.of(ID, OpenUnarchiverContext::class)

    private val supportedArchiveExtensions: Array<String> =
      // Keep in sync with `org.apache.commons.compress.compressors.gzip.GzipUtils`
      setOf("tgz", "taz", "svgz", "cpgz", "wmz", "emz", "gz", "z")
        .plus(
          ArchiveStreamFactory.findAvailableArchiveInputStreamProviders().map { it.key.lowercase() }
        )
        .sorted()
        .toTypedArray()

    private fun restoreFileAttributes(archiveEntry: ArchiveEntry, path: Path) {
      val attributes = Files.getFileAttributeView(path, BasicFileAttributeView::class.java)
      val oldAttributes = attributes.readAttributes()

      val fileTimes = FileTimes.fromArchiveEntry(archiveEntry)
      attributes.setTimes(
        fileTimes.lastModifiedTime ?: oldAttributes.lastModifiedTime(),
        fileTimes.lastAccessTime ?: oldAttributes.lastAccessTime(),
        fileTimes.creationTime ?: oldAttributes.creationTime(),
      )
    }
  }
}
