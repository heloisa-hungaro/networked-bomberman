//BOMBERWOMAN - SERVIDOR
//Autora: HELOISA HUNGARO PRIMOLAN
//RA: 141026431
//ago-2015


import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Servidor extends JFrame
{
	ImageIcon icone = new ImageIcon("figuras/bomba2.png");
	
	private JTextArea texto;
	private Jogador jogadores[] = new Jogador[2];
	private ServerSocket server;
	
	int destroix[] = new int[30];
	int destroiy[] = new int[30];
	int destroi[][] = new int[2][4];
	int bombas[][] = new int[2][2];
	int bposx[][] = new int[2][2];
	int bposy[][] = new int[2][2];
	int posx[] = new int[2];
	int posy[] = new int[2];
	int paraonde[] = new int[2];
	int ganhou[] = new int[2];
	int desistiu[] = new int[2];
	int saiu=0;
	String msg;
	
	public Servidor()
	{
		//cria janela
		super ("SERVIDOR BW");
		texto = new JTextArea();
		add(texto, BorderLayout.CENTER);
		this.setIconImage(icone.getImage());
		texto.setEditable(false);
		setSize(300,200);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true); 
		
		String portastr;
		int porta=0;
		boolean portavalida=false;
		
		//pede para o usuario digitar qual a porta
		while (!portavalida)
		{
			portastr=JOptionPane.showInputDialog("Digite a porta de conexao (Exemplo: 7890)","7890");
			if (portastr == null) //se o usuário cancelar digitação, sair do programa
				System.exit(0);
			try
			{
				porta=Integer.parseInt(portastr);
				portavalida=true;
			}
			catch (Exception e) //caso a porta não seja válida, mostra mensagem de erro e tenta novamente (while)
			{
				JOptionPane.showMessageDialog(this,"O valor de porta digitado eh invalido. Tente novamente. (Exemplo: 7890)","PORTA INVALIDA",JOptionPane.INFORMATION_MESSAGE);
			}
		}
		//tenta criar o servidor com a porta digitada
		try
		{
			server = new ServerSocket(porta,2);
		}
		catch (Exception e) //se não conseguir criar o servidor, porta inválida! fecha o programa
		{
			JOptionPane.showMessageDialog(this,"Nao foi possivel criar o servidor!\n"+e,"FALHA NA CONEXAO",JOptionPane.INFORMATION_MESSAGE);
			e.printStackTrace();
			System.exit(1);
		}
		
		for (int i=0;i<4;i++) //inicia vars
		{
			destroi[0][i]=-1;
			destroi[1][i]=-1;
		}
		desistiu[0]=-1;
		desistiu[1]=-1;
		
		
		//servidor criado! aguardando conexao dos clientes...
		texto.append("AGUARDANDO JOGADORES CONECTAREM...\n");	
		for (int i=0;i<2;i++) //só deixa dois clientes conectarem
		{
			try
			{
				jogadores[i] = new Jogador(server.accept(),i); //recebe conexao de cliente
				texto.append("JOGADOR "+(i+1)+" ENTROU.\n");
				jogadores[i].start(); //inicia a thread do jogador
			}
			catch (Exception e) //caso dê erro na conexao com um cliente, fecha programa
			{
				JOptionPane.showMessageDialog(this,"Nao foi possivel conectar com um dos clientes!\n"+e,"FALHA NA CONEXAO",JOptionPane.INFORMATION_MESSAGE);
				e.printStackTrace();
				System.exit(1);
			}
		}
		texto.append("JOGADORES CONECTADOS!\n\nJOGO EM ANDAMENTO!\n"); //iniciar o jogo!
		synchronized (jogadores[0]) //permite que o jogador que entrou antes finalmente comece a se mexer
		{
			jogadores[0].setSuspended(false);
			jogadores[0].notify();
		}
		try 
		{
			server.close();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	
	public static void main (String args[])
	{
		Servidor servidor = new Servidor();
	}
	
	
	private class Jogador extends Thread
	{
		private Socket cliente;
		private DataInputStream input;
		private DataOutputStream output;
		private int qualJogador;
		protected boolean suspended = true;
		
		public Jogador (Socket socket, int qual)
		{
			qualJogador = qual;
			cliente = socket;
			
			try
			{
				//cria input e output: só passo dados inteiros entre cliente e servidor
				input = new DataInputStream(cliente.getInputStream());
				output = new DataOutputStream(cliente.getOutputStream());
				output.writeInt(qualJogador); //manda para o cliente qual jogador ele é
				output.flush();
				ganhou[qualJogador]=-1;
				for (int i=0;i<30;i++) //ha 30 blocos destrutiveis
				{
					if (qual==0) //se é o primeiro jogador, recebe onde estao esses blocos
					{
						destroix[i] = input.readInt();
						destroiy[i] = input.readInt();
					}
					else //se é o segundo jogador, envia onde estão esses blocos
					{
						output.writeInt(destroix[i]);
						output.writeInt(destroiy[i]);
						output.flush();
					}
				}
			}
			catch (Exception e) //caso dê erro, mostra mensagem e fecha o programa
			{
				JOptionPane.showMessageDialog(null,"Erro ao iniciar um dos clientes\n"+e,"PROBLEMA NA CONEXAO",JOptionPane.INFORMATION_MESSAGE);
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		
		public void run() //thread que recebe e envia dados para o cliente
		{
			try
			{
				if(qualJogador == 0) //se for o jogador 1, aguardar conexao do 2
				{
					output.writeUTF("ESPERANDO ADVERSARIO...");
					output.flush();
					try
					{
						synchronized (this)
						{
							while (suspended)
								wait();
						}
					}
					catch (Exception e)
					{
						JOptionPane.showMessageDialog(null,"Erro ao iniciar o jogo\n"+e,"PROBLEMA NA CONEXAO",JOptionPane.INFORMATION_MESSAGE);
						e.printStackTrace();
						System.exit(1);
					}
					output.writeUTF("JOGUE!"); //finalmente começa o jogo!
					output.flush();
				}
				
				if (qualJogador==0)
				{
					
					do //enquanto nao acabar o jogo, troca informaçoes com o jogador 1
					{
						if (desistiu[1]==1)
							break;
						output.writeUTF("JOGADA");
						desistiu[0]=input.readInt();
						if (desistiu[0]==1)
							break;
						output.writeInt(posx[1]);
						output.writeInt(posy[1]);
						output.writeInt(paraonde[1]);
						output.flush();
						posx[0]=input.readInt();
						posy[0]=input.readInt();
						paraonde[0]=input.readInt();
						for (int i=0;i<4;i++) 
						{
							destroi[0][i]=input.readInt();
							output.writeInt(destroi[1][i]);
							output.flush();
						}
						for (int i=0;i<2;i++) 
						{
							bombas[0][i]=input.readInt();
							bposx[0][i]=input.readInt();
							bposy[0][i]=input.readInt();
							output.writeInt(bombas[1][i]);
							output.writeInt(bposx[1][i]);
							output.writeInt(bposy[1][i]);
							output.flush();
						}
						ganhou[0]=input.readInt();
						output.flush();
						try 
						{
								sleep (4);
						} catch (InterruptedException ex) {} 				
					} while(ganhou[0]==-1 && desistiu[0]==-1 && desistiu[1]==-1);
				}
				else
				{
					do //enquanto nao acabar o jogo, troca informaçoes com o jogador 2
					{
						if (desistiu[0]==1)
							break;
						output.writeUTF("JOGADA");
						desistiu[1]=input.readInt();
						if (desistiu[1]==1)
							break;
						output.writeInt(posx[0]);
						output.writeInt(posy[0]);
						output.writeInt(paraonde[0]);
						output.flush();
						posx[1]=input.readInt();
						posy[1]=input.readInt();
						paraonde[1]=input.readInt();
						for (int i=0;i<4;i++) 
						{
							destroi[1][i]=input.readInt();
							output.writeInt(destroi[0][i]);
						output.flush();
						}
						for (int i=0;i<2;i++) 
						{
							bombas[1][i]=input.readInt();
							bposx[1][i]=input.readInt();
							bposy[1][i]=input.readInt();
							output.writeInt(bombas[0][i]);
							output.writeInt(bposx[0][i]);
							output.writeInt(bposy[0][i]);
						output.flush();
						}
						ganhou[1]=input.readInt();
						output.flush();
						try 
						{
								sleep (4);
						} catch (InterruptedException ex) {} 				
					} while(ganhou[1]==-1 && desistiu[0]==-1 && desistiu[1]==-1);
				}
				saiu++; //acabou o jogo! 
				cliente.close(); //finaliza conexao do cliente
				//se o jogador 1 saiu antes do jogador 2 entrar, finaliza
				if (desistiu[0]==1)
				{
					texto.setText("");
					texto.append("FIM DE JOGO\n\n");
					texto.append("JOGADOR 1 DESISTIU\n\n");
					texto.append("JOGO ELABORADO POR: HELOISA HUNGARO\nRA 141026431\nago/2015\n\n");
					texto.append("SERVIDOR FINALIZADO\n");
				}
				else if (desistiu[1]==1)
				{
					texto.setText("");
					texto.append("FIM DE JOGO\n\n");
					texto.append("JOGADOR 2 DESISTIU\n\n");
					texto.append("JOGO ELABORADO POR: HELOISA HUNGARO\nRA 141026431\nago/2015\n\n");
					texto.append("SERVIDOR FINALIZADO\n");
				}
				else if (saiu==2) //se os dois clientes ja sairam, exibe resultado
				{
					texto.setText("");
					texto.append("FIM DE JOGO\n\n");
					if (ganhou[0]==1)
					{
						texto.append("JOGADOR 1 VENCEU\n\n");
					}
					else if (ganhou[1]==1)
					{
						texto.append("JOGADOR 2 VENCEU\n\n");
					}
					else
					{
						texto.append("EMPATOU\n\n");
					}
					texto.append("JOGO ELABORADO POR: HELOISA HUNGARO\nRA 141026431\nago/2015\n\n");
					texto.append("SERVIDOR FINALIZADO\n");
				}
				
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null,"Erro ao trocar dados com um dos clientes\n"+e,"PROBLEMA NA TROCA DE DADOS",JOptionPane.INFORMATION_MESSAGE);
				e.printStackTrace();
				System.exit(1);
			}
			
		}

		public void setSuspended (boolean status) //usada para manter o jogador 1 aguardando o jogador 2 conectar
		{
			suspended = status;
		}
		
	}
	
	
	
	
	
	
}
