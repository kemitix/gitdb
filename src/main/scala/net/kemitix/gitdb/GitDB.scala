package net.kemitix.gitdb

import java.nio.file.Path

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import scala.beans.BeanProperty

class GitDB (val dir: Path) {

  @BeanProperty
  val repository: Repository = {
    val fileRepositoryBuilder = new FileRepositoryBuilder
    fileRepositoryBuilder.setGitDir(dir.toFile)
    fileRepositoryBuilder.build()
  }

  def close: Unit = repository.close()

}

object GitDB {
  def init(dir: Path): GitDB = {
    new GitDB(dir)
  }
}