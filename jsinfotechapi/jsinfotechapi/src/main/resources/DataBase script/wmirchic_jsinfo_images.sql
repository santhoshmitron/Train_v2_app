-- MySQL dump 10.13  Distrib 5.7.27, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: wmirchic_jsinfo
-- ------------------------------------------------------
-- Server version	5.7.27-0ubuntu0.16.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `images`
--

DROP TABLE IF EXISTS `images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `images` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ids` int(11) NOT NULL,
  `pic` varchar(255) NOT NULL,
  `place` varchar(100) NOT NULL,
  `added_on` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=98 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `images`
--

LOCK TABLES `images` WRITE;
/*!40000 ALTER TABLE `images` DISABLE KEYS */;
INSERT INTO `images` VALUES (97,2,'1563162529barcelona47.jpg','trips','2019-07-14 23:48:48'),(96,2,'1563162529barcelona46.jpg','trips','2019-07-14 23:48:48'),(95,2,'1563162529barcelona45.jpg','trips','2019-07-14 23:48:48'),(94,2,'1563162529barcelona44.jpg','trips','2019-07-14 23:48:48'),(13,2,'1562600737barcelona00.jpg','trips','2019-07-08 11:45:36'),(14,2,'1562601246barcelona01.jpg','trips','2019-07-08 11:54:07'),(15,2,'1562601246barcelona02.jpg','trips','2019-07-08 11:54:07'),(16,2,'1562601247barcelona03.jpg','trips','2019-07-08 11:54:07'),(17,2,'1562601247barcelona04.jpg','trips','2019-07-08 11:54:07'),(18,2,'1562601247barcelona05.jpg','trips','2019-07-08 11:54:07'),(19,2,'1562601247barcelona06.jpg','trips','2019-07-08 11:54:07'),(20,2,'1562601247barcelona07.jpg','trips','2019-07-08 11:54:07'),(21,2,'1562601247barcelona08.jpg','trips','2019-07-08 11:54:07'),(22,2,'1562601247barcelona09.jpg','trips','2019-07-08 11:54:07'),(23,2,'1562601247barcelona10.jpg','trips','2019-07-08 11:54:07'),(24,2,'1562601247barcelona11.jpg','trips','2019-07-08 11:54:07'),(25,2,'1562601247barcelona12.jpg','trips','2019-07-08 11:54:07'),(26,2,'1562601247barcelona13.jpg','trips','2019-07-08 11:54:07'),(27,2,'1562601247barcelona14.jpg','trips','2019-07-08 11:54:07'),(28,2,'1562601247barcelona15.jpg','trips','2019-07-08 11:54:07'),(29,2,'1562601247barcelona16.jpg','trips','2019-07-08 11:54:07'),(30,2,'1562601247barcelona17.jpg','trips','2019-07-08 11:54:07'),(31,2,'1562601247barcelona18.jpg','trips','2019-07-08 11:54:07'),(32,2,'1562601247barcelona19.jpg','trips','2019-07-08 11:54:07'),(33,2,'1562601247barcelona20.jpg','trips','2019-07-08 11:54:07'),(34,2,'1563160613barcelona00.jpg','trips','2019-07-14 23:16:53'),(35,2,'1563160613barcelona01.jpg','trips','2019-07-14 23:16:53'),(36,2,'1563160613barcelona02.jpg','trips','2019-07-14 23:16:53'),(37,2,'1563160614barcelona03.jpg','trips','2019-07-14 23:16:53'),(38,2,'1563160614barcelona04.jpg','trips','2019-07-14 23:16:53'),(39,2,'1563160614barcelona05.jpg','trips','2019-07-14 23:16:53'),(40,2,'1563160614barcelona06.jpg','trips','2019-07-14 23:16:53'),(41,2,'1563160614barcelona07.jpg','trips','2019-07-14 23:16:53'),(42,2,'1563160614barcelona08.jpg','trips','2019-07-14 23:16:53'),(43,2,'1563160614barcelona09.jpg','trips','2019-07-14 23:16:53'),(44,2,'1563160614barcelona10.jpg','trips','2019-07-14 23:16:53'),(45,2,'1563160614barcelona11.jpg','trips','2019-07-14 23:16:53'),(46,2,'1563160614barcelona12.jpg','trips','2019-07-14 23:16:53'),(47,2,'1563160614barcelona13.jpg','trips','2019-07-14 23:16:53'),(48,2,'1563160614barcelona14.jpg','trips','2019-07-14 23:16:53'),(49,2,'1563160614barcelona15.jpg','trips','2019-07-14 23:16:53'),(50,2,'1563160614barcelona16.jpg','trips','2019-07-14 23:16:53'),(51,2,'1563160614barcelona17.jpg','trips','2019-07-14 23:16:53'),(52,2,'1563160614barcelona18.jpg','trips','2019-07-14 23:16:53'),(53,2,'1563160614barcelona19.jpg','trips','2019-07-14 23:16:53'),(93,9,'1563162368barcelona19.jpg','trips','2019-07-14 23:46:07'),(92,9,'1563162368barcelona18.jpg','trips','2019-07-14 23:46:07'),(91,9,'1563162368barcelona17.jpg','trips','2019-07-14 23:46:07'),(90,9,'1563162368barcelona16.jpg','trips','2019-07-14 23:46:07'),(89,9,'1563162368barcelona15.jpg','trips','2019-07-14 23:46:07'),(88,9,'1563162368barcelona14.jpg','trips','2019-07-14 23:46:07'),(87,9,'1563162368barcelona13.jpg','trips','2019-07-14 23:46:07'),(86,9,'1563162368barcelona12.jpg','trips','2019-07-14 23:46:07'),(85,9,'1563162368barcelona11.jpg','trips','2019-07-14 23:46:07'),(84,9,'1563162367barcelona10.jpg','trips','2019-07-14 23:46:07'),(83,9,'1563162367barcelona09.jpg','trips','2019-07-14 23:46:07'),(82,9,'1563162367barcelona08.jpg','trips','2019-07-14 23:46:07'),(81,9,'1563162367barcelona07.jpg','trips','2019-07-14 23:46:07'),(80,9,'1563162367barcelona06.jpg','trips','2019-07-14 23:46:07'),(79,9,'1563162367barcelona05.jpg','trips','2019-07-14 23:46:07'),(78,9,'1563162367barcelona04.jpg','trips','2019-07-14 23:46:07'),(77,9,'1563162367barcelona03.jpg','trips','2019-07-14 23:46:07'),(76,9,'1563162367barcelona02.jpg','trips','2019-07-14 23:46:07'),(75,9,'1563162367barcelona01.jpg','trips','2019-07-14 23:46:07'),(74,9,'1563162367barcelona00.jpg','trips','2019-07-14 23:46:07');
/*!40000 ALTER TABLE `images` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-10-24 13:56:57
