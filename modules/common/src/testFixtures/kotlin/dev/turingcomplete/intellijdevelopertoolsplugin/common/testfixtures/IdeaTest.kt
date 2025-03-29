package dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.junit5.RunMethodInEdt
import com.intellij.testFramework.junit5.TestApplication
import kotlin.reflect.KClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

@RunInEdt(allMethods = false)
@TestApplication
abstract class IdeaTest {
  // -- Properties ---------------------------------------------------------- //

  protected lateinit var fixture: IdeaProjectTestFixture
  protected lateinit var disposable: TestDisposable

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @BeforeEach
  fun beforeEach() {
    disposable = TestDisposable(this::class)
    fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test").fixture
    fixture.setUp()
  }

  @AfterEach
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  fun afterEach() {
    Disposer.dispose(disposable)
    fixture.tearDown()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class TestDisposable(val id: String) : Disposable {

    constructor(testClass: KClass<*>) : this(id = testClass.simpleName!!)

    override fun dispose() {}

    override fun toString(): String = id
  }

  // -- Companion Object ---------------------------------------------------- //
}
