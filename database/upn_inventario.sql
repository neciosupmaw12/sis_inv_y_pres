-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: upn_inventario
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `upn_inventario`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `upn_inventario` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `upn_inventario`;

--
-- Table structure for table `detalle_prestamos`
--

DROP TABLE IF EXISTS `detalle_prestamos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `detalle_prestamos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` int(11) NOT NULL,
  `equipo_id` bigint(20) NOT NULL,
  `prestamo_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjantgosj02i6yuu1sslur7fsf` (`equipo_id`),
  KEY `FKee71bmwo65o9j07vp7ak0st9t` (`prestamo_id`),
  CONSTRAINT `FKee71bmwo65o9j07vp7ak0st9t` FOREIGN KEY (`prestamo_id`) REFERENCES `prestamos` (`id`),
  CONSTRAINT `FKjantgosj02i6yuu1sslur7fsf` FOREIGN KEY (`equipo_id`) REFERENCES `equipos` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `detalle_prestamos`
--

LOCK TABLES `detalle_prestamos` WRITE;
/*!40000 ALTER TABLE `detalle_prestamos` DISABLE KEYS */;
INSERT INTO `detalle_prestamos` VALUES (3,1,2,2),(4,1,6,2),(5,3,8,3),(6,1,2,3),(7,1,8,4),(9,1,8,6),(10,2,5,6);
/*!40000 ALTER TABLE `detalle_prestamos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `equipos`
--

DROP TABLE IF EXISTS `equipos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `equipos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad_disponible` int(11) NOT NULL,
  `cantidad_total` int(11) NOT NULL,
  `categoria` varchar(50) NOT NULL,
  `codigo` varchar(20) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `estado` enum('DE_BAJA','EN_MANTENIMIENTO','OPERATIVO') NOT NULL,
  `marca` varchar(50) DEFAULT NULL,
  `modelo` varchar(50) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `version` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKorutdsib6gh64c5a0997cd1k0` (`codigo`),
  KEY `idx_equipos_nombre` (`nombre`),
  KEY `idx_equipos_estado_categoria` (`estado`,`categoria`),
  KEY `idx_equipos_categoria` (`categoria`),
  KEY `idx_equipos_marca` (`marca`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `equipos`
--

LOCK TABLES `equipos` WRITE;
/*!40000 ALTER TABLE `equipos` DISABLE KEYS */;
INSERT INTO `equipos` VALUES (1,10,10,'Laptops','LAP-001',NULL,'OPERATIVO','Lenovo','ThinkPad E14 Gen 5','Laptop Lenovo ThinkPad E14',0),(2,6,6,'Laptops','LAP-002',NULL,'OPERATIVO','HP','ProBook 450 G10','Laptop HP ProBook 450',0),(3,5,5,'Proyectores','PRY-001',NULL,'OPERATIVO','Epson','PowerLite X49','Proyector multimedia Epson',0),(4,8,8,'Tablets','TAB-001',NULL,'OPERATIVO','Samsung','Galaxy Tab A9+','Tablet Samsung Galaxy Tab',0),(5,10,12,'Periféricos','CAM-001',NULL,'OPERATIVO','Logitech','C920','Cámara web Full HD',0),(6,10,10,'Audio','AUD-001',NULL,'OPERATIVO','Shure','BLX24','Kit de micrófono inalámbrico',0),(8,8,10,'Laptops','LAP-009','PRUEBA','OPERATIVO','ASUS','ASUS','laptop asus',0);
/*!40000 ALTER TABLE `equipos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prestamos`
--

DROP TABLE IF EXISTS `prestamos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `prestamos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `estado` varchar(20) NOT NULL,
  `fecha_devolucion_prevista` date NOT NULL,
  `fecha_devolucion_real` datetime(6) DEFAULT NULL,
  `fecha_prestamo` datetime(6) NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `usuario_id` bigint(20) NOT NULL,
  `solicitante_id` bigint(20) NOT NULL,
  `fecha_aprobacion` datetime(6) DEFAULT NULL,
  `fecha_solicitud` datetime(6) DEFAULT NULL,
  `motivo_rechazo` varchar(255) DEFAULT NULL,
  `aprobado_por_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeqd1t799y0x5ck9mdeltepy1w` (`usuario_id`),
  KEY `FKt4q0sp8cb3gjh0wtkt3fnkbbf` (`solicitante_id`),
  KEY `idx_prestamos_estado_fecha_prevista` (`estado`,`fecha_devolucion_prevista`),
  KEY `idx_prestamos_fecha_prestamo` (`fecha_prestamo`),
  KEY `FK2dc7vb4bg0fwu19ndyw9wp6xr` (`aprobado_por_id`),
  CONSTRAINT `FK2dc7vb4bg0fwu19ndyw9wp6xr` FOREIGN KEY (`aprobado_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `FKeqd1t799y0x5ck9mdeltepy1w` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `FKt4q0sp8cb3gjh0wtkt3fnkbbf` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitantes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prestamos`
--

LOCK TABLES `prestamos` WRITE;
/*!40000 ALTER TABLE `prestamos` DISABLE KEYS */;
INSERT INTO `prestamos` VALUES (2,'DEVUELTO','2026-11-09','2026-11-02 16:50:43.000000','2026-11-02 16:09:39.000000','laboratorio 305 , clase redes 2.',1,3,NULL,NULL,NULL,NULL),(3,'DEVUELTO','2026-11-09','2026-11-02 17:17:59.000000','2026-11-02 17:17:08.000000',NULL,1,3,NULL,NULL,NULL,NULL),(4,'ACTIVO','2026-11-09',NULL,'2026-11-02 17:17:41.000000','LAB 230 - REDES 2',1,1,NULL,NULL,NULL,NULL),(6,'ACTIVO','2026-11-17',NULL,'2026-11-10 21:59:29.000000','clase de soluciones web',1,4,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `prestamos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitantes`
--

DROP TABLE IF EXISTS `solicitantes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `solicitantes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `apellidos` varchar(80) NOT NULL,
  `area` varchar(100) DEFAULT NULL,
  `dni` varchar(8) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `nombres` varchar(80) NOT NULL,
  `telefono` varchar(9) DEFAULT NULL,
  `tipo` enum('ADMINISTRATIVO','DOCENTE','ESTUDIANTE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKoanjqdkgcj1e8jhyaybnu3tgt` (`dni`),
  UNIQUE KEY `UKm4kus3ttihn008k8tpiuqrh5w` (`email`),
  KEY `idx_solicitantes_apellidos_nombres` (`apellidos`,`nombres`),
  KEY `idx_solicitantes_tipo` (`tipo`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitantes`
--

LOCK TABLES `solicitantes` WRITE;
/*!40000 ALTER TABLE `solicitantes` DISABLE KEYS */;
INSERT INTO `solicitantes` VALUES (1,'Quispe Torres','Ingeniería de Sistemas','70123456','maria.quispe@upn.pe','María','987654321','ESTUDIANTE'),(2,'Ramírez Soto','Facultad de Ingeniería','45678912','carlos.ramirez@upn.pe','Carlos','912345678','DOCENTE'),(3,'perez','ing sistemas','87654321','n00123546@upn.pe','juan','987654321','ESTUDIANTE'),(4,'perez','ing sistemas','87654320','sistemas@sistemas.com','juancito','987654321','ESTUDIANTE'),(6,'Prueba UPN','Ingeniería de Sistemas','71234567','alumno.prueba@upn.pe','Alumno','951234567','ESTUDIANTE');
/*!40000 ALTER TABLE `solicitantes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usuarios` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` varchar(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `solicitante_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2dvbwfge291euvmk6vkkocao` (`username`),
  UNIQUE KEY `UK9wxv42qa4lal44ef3fa3w4mxp` (`solicitante_id`),
  CONSTRAINT `FKkejbc2qaq0jnwaraf2ff1a38e` FOREIGN KEY (`solicitante_id`) REFERENCES `solicitantes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'','Administrador','$2a$10$08ey8TTs/0P6aX6h.PhK5OFw45s0o9kjpQ9lAHjowmi/7t1oewofG','ADMIN','admin',NULL),(2,'','Encargado de Préstamos','$2a$10$PsYA4h4JKhgFUCcT5JnMRubE15lD9WxbXXD69EQinoDBs8EuC.7FG','ENCARGADO','encargado',NULL),(3,'','Leslie Fernández Toro','$2a$10$Vp.MXYq5Ba4rav4Kj2.ROO54a4kgUZrzxFvFaz7ufmDkEdwhZcwOS','ADMIN','lfernandez',NULL),(4,'','Rodolfo Quispe Quispe','$2a$10$L2rmf9fxh.gzDLJk5KFMOu1XlZsaWSd0JMo.hs9RZdmG02uOhnycG','ADMIN','rquispe',NULL),(5,'','Marco Neciosup Panibra','$2a$10$VF2/6zYGQin0hj41kcsmBORL4UbC6KZPpV8E/XvMkS/mEMFtZvUTO','ADMIN','mneciosup',NULL),(6,'','Nelson Raúl Troncos Pintado','$2a$10$3g0vDOgb.pV.4mXtPo8Pyu/77NE8git.mNmVVI92L595XYQzxhLXC','ADMIN','ntroncos',NULL),(7,'','Juan Carlos Morales Rojas','$2a$10$tedMKBesjGPv3sgYl.nXce.QXkvWWLdRbxb6mwt9SucMG25ApuSTW','ADMIN','jmorales',NULL),(8,'','Alumno Prueba UPN','$2a$10$K2NTlWNT8bMiL8x/0LdSvulVa/TOJLvj8lk2HJ3vpljgW2yajqbOu','ALUMNO','alumno',6);
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'upn_inventario'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
