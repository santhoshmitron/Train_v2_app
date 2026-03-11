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
-- Table structure for table `managegates`
--

DROP TABLE IF EXISTS `managegates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `managegates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Gate_Num` varchar(255) NOT NULL,
  `BOOM1_ID` varchar(100) NOT NULL,
  `BOOM2_ID` varchar(100) NOT NULL,
  `handle` varchar(100) NOT NULL,
  `SM` varchar(100) NOT NULL,
  `GM` varchar(100) NOT NULL,
  `status` varchar(100) NOT NULL DEFAULT 'Open',
  `added_on` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `go` varchar(11) NOT NULL,
  `gc` varchar(11) NOT NULL,
  `ho` varchar(11) NOT NULL,
  `hc` varchar(11) NOT NULL,
  `gate_status` varchar(20) NOT NULL DEFAULT 'open',
  `handle_status` varchar(20) NOT NULL DEFAULT 'open',
  PRIMARY KEY (`id`),
  UNIQUE KEY `BOOM1_ID` (`BOOM1_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=14 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `managegates`
--

LOCK TABLES `managegates` WRITE;
/*!40000 ALTER TABLE `managegates` DISABLE KEYS */;
INSERT INTO `managegates` VALUES (1,'LC 1','1','2','sm1','gm1','Open','2019-07-31 12:38:31','530','520','500','540','close','closed'),(2,'LC 2','3','4','sm1','gm2','Closed','2019-07-31 13:19:24','530','520','500','540','closed','closed'),(3,'LC 3','5','6','sm1','gm2','Open','2019-08-02 17:13:54','530','520','500','540','open','open'),(9,'lc4','7','8','jsipl','Test','Open','2019-09-18 14:30:51','','','','','open','open'),(11,'lc5','9','10','jsipl','Test1','Open','2019-09-18 14:38:50','','','','','open','open');
/*!40000 ALTER TABLE `managegates` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-10-24 13:56:58
