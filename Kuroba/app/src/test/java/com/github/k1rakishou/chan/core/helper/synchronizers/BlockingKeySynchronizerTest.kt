package com.github.k1rakishou.chan.core.helper.synchronizers

import com.github.k1rakishou.chan.core.synchronizers.BlockingKeySynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class BlockingKeySynchronizerTest {

  @Test
  fun `should not deadlock when locking with different keys`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    cacheHandlerSynchronizer.withLocalLock("1") {
      cacheHandlerSynchronizer.withLocalLock("2") {
        cacheHandlerSynchronizer.withLocalLock("3") {
          cacheHandlerSynchronizer.withLocalLock("4") {
            ++value
          }
        }
      }
    }

    Assert.assertEquals(1, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `should not deadlock when nested locking with the same key`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    cacheHandlerSynchronizer.withLocalLock("1") {
      cacheHandlerSynchronizer.withLocalLock("1") {
        ++value
      }
    }

    Assert.assertEquals(1, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `should not deadlock when nested locking with global lock`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    cacheHandlerSynchronizer.withGlobalLock {
      cacheHandlerSynchronizer.withGlobalLock {
        ++value
      }
    }

    Assert.assertEquals(1, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `should not deadlock when mixing local and global locks`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    cacheHandlerSynchronizer.withGlobalLock {
      cacheHandlerSynchronizer.withLocalLock("1") {
        cacheHandlerSynchronizer.withGlobalLock {
          cacheHandlerSynchronizer.withLocalLock("1") {
            cacheHandlerSynchronizer.withGlobalLock {
              cacheHandlerSynchronizer.withLocalLock("2") {
                cacheHandlerSynchronizer.withLocalLock("3") {
                  ++value
                }
              }
            }
          }
        }
      }
    }

    Assert.assertEquals(1, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `should not allow locking a local lock when a global lock is already locked`() = runTest {
    var value = 0
    val startTime = System.currentTimeMillis()
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    coroutineScope {
      launch(Dispatchers.IO) {
        cacheHandlerSynchronizer.withGlobalLock {
          while (System.currentTimeMillis() - startTime < 10) {
            Assert.assertFalse(cacheHandlerSynchronizer.isLocalLockLocked("1"))
            Assert.assertEquals(value, 0)
          }
        }
      }

      launch(Dispatchers.IO) {
        cacheHandlerSynchronizer.withLocalLock("1") {
          ++value
        }
      }
    }

    Assert.assertEquals(value, 1)
  }

  @Test
  fun `concurrent access from multiple threads only local`() = runTest {
    val values = IntArray(50) { 0 }
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    (0 until 50).map { id ->
      async(Dispatchers.IO) {
        repeat(100) {
          cacheHandlerSynchronizer.withLocalLock(key = id.toString()) {
            values[id] = values[id] + 1
          }
        }
      }
    }.awaitAll()

    Assert.assertEquals(50 * 100, values.sum())
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `concurrent access from multiple threads only global`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    (0 until 50).map { id ->
      async(Dispatchers.IO) {
        repeat(100) {
          cacheHandlerSynchronizer.withGlobalLock {
            ++value
          }
        }
      }
    }.awaitAll()

    Assert.assertEquals(50 * 100, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `concurrent access from multiple threads mixed 1`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    (0 until 50).map { id ->
      async(Dispatchers.IO) {
        repeat(100) {
          cacheHandlerSynchronizer.withGlobalLock {
            cacheHandlerSynchronizer.withLocalLock(key = id.toString()) {
              ++value
            }
          }
        }
      }
    }.awaitAll()

    Assert.assertEquals(50 * 100, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

  @Test
  fun `concurrent access from multiple threads mixed 2`() = runTest {
    var value = 0
    val cacheHandlerSynchronizer = BlockingKeySynchronizer<String>()

    (0 until 50).map { id ->
      async(Dispatchers.IO) {
        repeat(100) {
          cacheHandlerSynchronizer.withLocalLock(key = id.toString()) {
            cacheHandlerSynchronizer.withGlobalLock {
              ++value
            }
          }
        }
      }
    }.awaitAll()

    Assert.assertEquals(50 * 100, value)
    Assert.assertTrue(cacheHandlerSynchronizer.getHeldLockKeys().isEmpty())
  }

}
