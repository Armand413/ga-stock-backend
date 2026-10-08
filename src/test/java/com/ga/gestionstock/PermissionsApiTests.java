package com.ga.gestionstock;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
class PermissionsApiTests extends ApiTestSupport {
 @Test void deleguerStockSansGestionComptesEtRevoquer() throws Exception {
  String name=unique(); long id=utilisateur(name,"LECTEUR").path("id").asLong();
  String old=body(connexion(name,PASSWORD)).path("accessToken").asString();
  assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of("GERER_STOCK")),old).statusCode()).isEqualTo(403);
  assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of("GERER_STOCK")),adminToken).statusCode()).isEqualTo(200);
  assertThat(req("GET","/api/auth/me",null,old).statusCode()).isEqualTo(401);
  var login=body(connexion(name,PASSWORD)); String token=login.path("accessToken").asString();
  assertThat(login.path("utilisateur").path("role").asString()).isEqualTo("LECTEUR");
  assertThat(login.path("utilisateur").path("permissions").toString()).contains("GERER_STOCK");
  long article=article(0);
  assertThat(req("GET","/api/articles",null,token).statusCode()).isEqualTo(200);
  assertThat(req("POST","/api/articles/"+article+"/mouvements",Map.of("type","ENTREE","quantite",2),token).statusCode()).isEqualTo(201);
  assertThat(req("POST","/api/articles",Map.of("reference",unique(),"nom","Test","unite","piece","seuilAlerte",0),token).statusCode()).isEqualTo(403);
  for(String path:List.of("/api/utilisateurs","/api/articles/export","/api/mouvements","/api/demandes","/api/tableau-bord","/api/alertes","/api/courriels"))
   assertThat(req("GET",path,null,token).statusCode()).as(path).isEqualTo(403);
  assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of()),adminToken).statusCode()).isEqualTo(200);
  assertThat(req("GET","/api/articles",null,token).statusCode()).isEqualTo(401);
  token=body(connexion(name,PASSWORD)).path("accessToken").asString();
  assertThat(req("GET","/api/articles",null,token).statusCode()).isEqualTo(403);
  assertThat(req("GET","/api/catalogue",null,token).statusCode()).isEqualTo(200);
 }
 @Test void deleguerDecisionSurDemandeAutrui() throws Exception {
  String owner=unique(),manager=unique(); utilisateur(owner,"LECTEUR");long id=utilisateur(manager,"LECTEUR").path("id").asLong();
  String ownerToken=body(connexion(owner,PASSWORD)).path("accessToken").asString();long article=article(0);mouvement(article,"ENTREE",4);
  long demande=body(req("POST","/api/demandes",Map.of("articleId",article,"quantite",2),ownerToken)).path("id").asLong();
  req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of("TRAITER_DEMANDES")),adminToken);
  String token=body(connexion(manager,PASSWORD)).path("accessToken").asString();
  assertThat(req("GET","/api/demandes",null,token).statusCode()).isEqualTo(200);
  assertThat(req("GET","/api/demandes/"+demande,null,token).statusCode()).isEqualTo(200);
  assertThat(req("PATCH","/api/demandes/"+demande+"/decision",Map.of("statut","APPROUVEE","reponse","Accord"),token).statusCode()).isEqualTo(200);
  assertThat(body(req("GET","/api/articles/"+article,null,adminToken)).path("quantite").asLong()).isEqualTo(2);
  assertThat(req("POST","/api/articles/"+article+"/mouvements",Map.of("type","ENTREE","quantite",1),token).statusCode()).isEqualTo(403);
 }
 @Test void verifierChaquePermissionEtValeursInvalides() throws Exception {
  var routes=Map.of("VOIR_TABLEAU_BORD","/api/tableau-bord","GERER_ARTICLES","/api/articles","VOIR_MOUVEMENTS","/api/mouvements","GERER_ALERTES","/api/alertes","EXPORTER_STOCK","/api/articles/export","GERER_NOTIFICATIONS","/api/courriels");
  String name=unique();long id=utilisateur(name,"LECTEUR").path("id").asLong();
  for(var e:routes.entrySet()) {
   assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of(e.getKey())),adminToken).statusCode()).isEqualTo(200);
   String token=body(connexion(name,PASSWORD)).path("accessToken").asString();
   assertThat(req("GET",e.getValue(),null,token).statusCode()).as(e.getKey()).isEqualTo(200);
   assertThat(req("GET","/api/utilisateurs",null,token).statusCode()).isEqualTo(403);
  }
  assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of("permissions",List.of("ADMIN")),adminToken).statusCode()).isEqualTo(400);
  assertThat(req("PUT","/api/utilisateurs/"+id+"/permissions",Map.of(),adminToken).statusCode()).isEqualTo(400);
  assertThat(req("PUT","/api/utilisateurs/"+adminId+"/permissions",Map.of("permissions",List.of()),adminToken).statusCode()).isEqualTo(409);
 }
}
