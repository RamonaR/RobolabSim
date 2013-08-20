package tud.robolab.view

import java.io.File
import javax.swing._
import java.awt.{GridLayout, BorderLayout}
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.event.{ChangeEvent, ChangeListener}
import tud.robolab.model.{Direction, Point, Maze}
import tud.robolab.utils.IOUtils
import spray.json._
import tud.robolab.model.MazeJsonProtocol._

class MazeGenerator extends JPanel {
  private var model: Maze = null

  private var curr_width = 6
  private var curr_height = 6

  private val name = new JTextField("maze")
  private val box = new JComboBox(IOUtils.getFileTreeFilter(new File("maps/"), ".maze"))
  private val spinnerx = new JSpinner(new SpinnerNumberModel(curr_width, 2, 12, 1))
  private val spinnery = new JSpinner(new SpinnerNumberModel(curr_height, 2, 12, 1))

  private val settings = buildSettingsPanel

  private var content = new JScrollPane(buildMazePanel())

  private val mapsPanel = buildMapsPanel

  setLayout(new BorderLayout())
  add(settings, BorderLayout.WEST)
  add(content, BorderLayout.CENTER)
  add(mapsPanel, BorderLayout.EAST)

  IOUtils.createDirectory(new File("maps/"))

  private def refresh {
    val listeners = box.getActionListeners
    box.removeActionListener(listeners(0))
    box.removeAllItems()
    IOUtils.getFileTreeFilter(new File("maps/"), ".maze").foreach(box.addItem(_))
    box.addActionListener(listeners(0))
  }

  private def buildMapsPanel: JPanel = {
    val result = new JPanel()
    result.setLayout(new BorderLayout())
    result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))

    box.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val box = e.getSource.asInstanceOf[JComboBox[String]]
        if (box.getSelectedIndex != -1) {
          val n = box.getSelectedItem.asInstanceOf[String]
          model = IOUtils.readFromFile(new File("maps/" + n + ".maze")).asJson.convertTo[Maze]
          curr_width = model.width
          curr_height = model.height
          spinnerx.setValue(curr_width)
          spinnery.setValue(curr_height)
          name.setText(n)
          rebuild()
        }
      }
    })

    val reloadBtn = new JButton("Reload maps")
    reloadBtn.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        refresh
      }
    })

    result.add(box, BorderLayout.NORTH)
    result.add(reloadBtn, BorderLayout.SOUTH)
    result
  }

  private def buildSettingsPanel: JPanel = {
    val labelx = new JLabel("Width ")
    val labely = new JLabel("Height ")
    val labeln = new JLabel("Name ")

    spinnerx.addChangeListener(new ChangeListener {
      def stateChanged(e: ChangeEvent) {
        curr_width = spinnerx.getModel.asInstanceOf[SpinnerNumberModel].getNumber.intValue()
        rebuild()
      }
    })

    spinnery.addChangeListener(new ChangeListener {
      def stateChanged(e: ChangeEvent) {
        curr_height = spinnery.getModel.asInstanceOf[SpinnerNumberModel].getNumber.intValue()
        rebuild()
      }
    })

    val edit = new JPanel(new GridLayout(3, 2, 5, 10))
    edit.add(labelx)
    edit.add(spinnerx)
    edit.add(labely)
    edit.add(spinnery)
    edit.add(labeln)
    edit.add(name)

    val result = new JPanel(new BorderLayout())
    result.add(edit, BorderLayout.NORTH)

    val okbtn = new JButton("Generate")
    okbtn.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        IOUtils.writeToFile("maps/" + name.getText + ".maze", model.toJson.prettyPrint)
        refresh
      }
    })

    result.add(okbtn, BorderLayout.SOUTH)
    result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    result
  }

  private def rebuild() {
    invalidate()
    remove(content)
    content = new JScrollPane(buildMazePanel())
    add(content, BorderLayout.CENTER)
    validate()
  }

  private def buildMazePanel(): JPanel = {
    val result = new JPanel()
    result.setLayout(new GridLayout(curr_width, curr_height, 5, 5))

    if (model == null || curr_height != model.height || curr_width != model.width)
      model = Maze.empty(curr_width, curr_height)

    model.points.flatten.foreach(p => result.add(new Tile(p.get)))

    result
  }
}
