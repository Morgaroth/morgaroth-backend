package io.github.morgaroth.phonefetcher

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.language.postfixOps
import better.files._
import better.files.Dsl.SymbolicOperations
import better.files.Dsl._

class DirectoriesTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  val dir = file"testDirectory"


  "Move command" should "merge files in directory" in {
    val source = dir / "source"
    val target = dir / "target"

    source / "dir1" createDirectories()
    source / "dir2" createDirectory()

    target / "dir2" createDirectories()
    target / "dir3" createDirectory()

    source / "dir1" / "file1" << "test content of first file"
    source / "dir2" / "file2" << "test content of second file"
    source / "dir2" / "file3" << "test content of third file"

    target / "dir2" / "file4" << "test content of fourth file"

    Directories.merge(source, target)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dir.delete()
    dir.createDirectory()
  }

  //  override protected def afterEach(): Unit = {
  //    dir.delete()
  //    super.afterEach()
  //  }
}